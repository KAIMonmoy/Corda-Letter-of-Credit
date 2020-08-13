package com.example.server;

import com.example.flow.*;
import com.example.state.BillOfLadingState;
import com.example.state.LetterOfCreditState;
import com.example.state.PurchaseOrderState;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api/example/") // The paths for HTTP requests are relative to this base path.
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(RestController.class);
    private final CordaRPCOps proxy;
    private final CordaX500Name me;

    public MainController(NodeRPCConnection rpc) {
        this.proxy = rpc.getProxy();
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();

    }

    /** Helpers for filtering the network map cache. */
    public String toDisplayString(X500Name name){
        return BCStyle.INSTANCE.toString(name);
    }

    private boolean isNotary(NodeInfo nodeInfo) {
        return !proxy.notaryIdentities()
                .stream().filter(el -> nodeInfo.isLegalIdentity(el))
                .collect(Collectors.toList()).isEmpty();
    }

    private boolean isMe(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private boolean isNetworkMap(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    @Configuration
    class Plugin {
        @Bean
        public ObjectMapper registerModule() {
            return JacksonSupport.createNonRpcMapper();
        }
    }

    @GetMapping(value = "/status", produces = TEXT_PLAIN_VALUE)
    private String status() {
        return "200";
    }

    @GetMapping(value = "/servertime", produces = TEXT_PLAIN_VALUE)
    private String serverTime() {
        return (LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC"))).toString();
    }

    @GetMapping(value = "/addresses", produces = TEXT_PLAIN_VALUE)
    private String addresses() {
        return proxy.nodeInfo().getAddresses().toString();
    }

    @GetMapping(value = "/identities", produces = TEXT_PLAIN_VALUE)
    private String identities() {
        return proxy.nodeInfo().getLegalIdentities().toString();
    }

    @GetMapping(value = "/platformversion", produces = TEXT_PLAIN_VALUE)
    private String platformVersion() {
        return Integer.toString(proxy.nodeInfo().getPlatformVersion());
    }

    @GetMapping(value = "/peers", produces = APPLICATION_JSON_VALUE)
    public HashMap<String, List<String>> getPeers() {
        HashMap<String, List<String>> myMap = new HashMap<>();

        // Find all nodes that are not notaries, ourself, or the network map.
        Stream<NodeInfo> filteredNodes = proxy.networkMapSnapshot().stream()
                .filter(el -> !isNotary(el) && !isMe(el) && !isNetworkMap(el));
        // Get their names as strings
        List<String> nodeNames = filteredNodes.map(el -> el.getLegalIdentities().get(0).getName().toString())
                .collect(Collectors.toList());

        myMap.put("peers", nodeNames);
        return myMap;
    }

    @GetMapping(value = "/notaries", produces = TEXT_PLAIN_VALUE)
    private String notaries() {
        return proxy.notaryIdentities().toString();
    }

    @GetMapping(value = "/flows", produces = TEXT_PLAIN_VALUE)
    private String flows() {
        return proxy.registeredFlows().toString();
    }

    @GetMapping(value = "/states", produces = TEXT_PLAIN_VALUE)
    private String states() {
        return proxy.vaultQuery(ContractState.class).getStates().toString();
    }

    @GetMapping(value = "/me",produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whoami(){
        HashMap<String, String> myMap = new HashMap<>();
        myMap.put("me", me.toString());
        return myMap;
    }

    // ---------------------------- Letter-Of-Credit ------------------------------------------------


    @GetMapping(value = "/transaction",produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getAllTransactions() {
        // Collect all POs
        List<StateAndRef<PurchaseOrderState>> unconsumedPurchaseOrders = proxy.vaultQueryByCriteria(
                new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED),
                PurchaseOrderState.class).getStates();
        List<StateAndRef<PurchaseOrderState>> consumedPurchaseOrders = proxy.vaultQueryByCriteria(
                new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED),
                PurchaseOrderState.class).getStates();

        // Collect all LCs
        List<StateAndRef<LetterOfCreditState>> unconsumedLetterOfCredits = proxy.vaultQueryByCriteria(
                new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED),
                LetterOfCreditState.class).getStates();

        // Collect all BLs
        List<StateAndRef<BillOfLadingState>> unconsumedBillOfLadings = proxy.vaultQueryByCriteria(
                new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED),
                BillOfLadingState.class).getStates();


        try {
            List<HashMap<String, List>> transactions = new ArrayList<>();
            unconsumedPurchaseOrders.forEach(
                    it -> {
                        HashMap<String, List> tx = new HashMap<>();
                        tx.put("states", Arrays.asList(it, null, null));
                        transactions.add(tx);
                    }
            );

            unconsumedLetterOfCredits.forEach(
                    it -> {
                        HashMap<String, List> tx = new HashMap<>();

                        final String poId = it.getState().getData().getPurchaseOrderId();
                        final String bolId = it.getState().getData().getBillOfLadingId();
                        final String locStatus = it.getState().getData().getLocStatus();
                        // locStatus LifeCycle
                        // APPLIED -> REJECTED
                        // APPLIED -> ISSUED -> SHIPPED -> SELLER_PAID -> ADVISING_BANK_PAID -> ISSUING_BANK_PAID
                        final boolean isBillAvailable =
                                !(locStatus.equals("APPLIED") || locStatus.equals("ISSUED") || locStatus.equals("REJECTED"));

                        tx.put("states", Arrays.asList(
                                consumedPurchaseOrders.stream()
                                        .filter(itr -> itr.getState().getData().getPurchaseOrderId().equals(poId))
                                        .collect(Collectors.toList()).get(0),
                                it,
                                isBillAvailable ? unconsumedBillOfLadings.stream()
                                        .filter(itr -> itr.getState().getData().getBillOfLadingId().equals(bolId))
                                        .collect(Collectors.toList()).get(0)
                                        : null));
                        transactions.add(tx);
                    }
            );

            return ResponseEntity.ok(transactions);

        } catch (Exception ex) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Failed to fetch transactions. " + ex.getMessage());
        }
    }

    @GetMapping(value = "/transaction/po/{poId}",produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getTransactionByPoId(@PathVariable String poId) {
        try {
            StateAndRef<PurchaseOrderState> unconsumedPurchaseOrder = proxy.vaultQueryByCriteria(
                    new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED),
                    PurchaseOrderState.class).getStates().stream()
                    .filter(it -> it.getState().getData().getPurchaseOrderId().equals(poId))
                    .collect(Collectors.toList()).get(0);
            HashMap<String, List> tx = new HashMap<>();
            tx.put("states", Arrays.asList(unconsumedPurchaseOrder, null, null));

            return ResponseEntity.ok(tx);
        } catch (Exception ex) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Failed to fetch purchase order. " + ex.getMessage());
        }
    }

    @GetMapping(value = "/transaction/loc/{locId}",produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getTransactionByLocId(@PathVariable String locId) {
        try {
            // Collect LC
            StateAndRef<LetterOfCreditState> unconsumedLetterOfCredit = proxy.vaultQueryByCriteria(
                    new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED),
                    LetterOfCreditState.class).getStates().stream()
                    .filter(it -> it.getState().getData().getLocId().equals(locId))
                    .collect(Collectors.toList()).get(0);

            final String poId = unconsumedLetterOfCredit.getState().getData().getPurchaseOrderId();
            final String bolId = unconsumedLetterOfCredit.getState().getData().getBillOfLadingId();
            final String locStatus = unconsumedLetterOfCredit.getState().getData().getLocStatus();
            // locStatus LifeCycle
            // APPLIED -> REJECTED
            // APPLIED -> ISSUED -> SHIPPED -> SELLER_PAID -> ADVISING_BANK_PAID -> ISSUING_BANK_PAID

            final boolean isBillAvailable =
                    !(locStatus.equals("APPLIED") || locStatus.equals("ISSUED") || locStatus.equals("REJECTED"));

            // Collect PO
            StateAndRef<PurchaseOrderState> consumedPurchaseOrder = proxy.vaultQueryByCriteria(
                    new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED),
                    PurchaseOrderState.class).getStates().stream()
                    .filter(it -> it.getState().getData().getPurchaseOrderId().equals(poId))
                    .collect(Collectors.toList()).get(0);

            // Collect all BLs
            StateAndRef<BillOfLadingState> unconsumedBillOfLading = isBillAvailable ? proxy.vaultQueryByCriteria(
                    new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED),
                    BillOfLadingState.class).getStates().stream()
                    .filter(it -> it.getState().getData().getBillOfLadingId().equals(bolId))
                    .collect(Collectors.toList()).get(0) : null;

            HashMap<String, List> tx = new HashMap<>();
            tx.put("states", Arrays.asList(
                    consumedPurchaseOrder,
                    unconsumedLetterOfCredit,
                    unconsumedBillOfLading)
            );

            return ResponseEntity.ok(tx);
        } catch (Exception ex) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Failed to fetch states by id. " + ex.getMessage());
        }
    }

    @PostMapping("/create-purchase-order")
    @ResponseBody
    public ResponseEntity createPurchaseOrder(@RequestBody HashMap<String, Object> form) throws IllegalArgumentException {

        Party buyerParty = proxy.wellKnownPartyFromX500Name(
                CordaX500Name.parse(String.valueOf(form.get("buyer")))
        );

        try {

            SignedTransaction result = proxy.startFlowDynamic(
                    CreatePurchaseOrderFlow.Initiator.class,
                    buyerParty,
                    form.get("purchaseOrderIssueDate"),
                    form.get("productName"),
                    Long.valueOf(form.get("productQuantity").toString()),
                    Long.valueOf(form.get("productPriceInUSD").toString()),
                    Long.valueOf(form.get("productGrossWeightInKG").toString())
            ).getReturnValue().get();
            // Return the response.
            final HashMap<String, Object> response = new HashMap<>();
            response.put("tx_id", result.getId());
            response.put("purchase_order", result.getTx().getOutput(0));
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception ex) {
            final HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to complete flow. " + ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }

    @PostMapping("/apply-for-loc")
    @ResponseBody
    public ResponseEntity applyForLetterOfCredit(@RequestBody HashMap<String, Object> form) throws IllegalArgumentException {

        Party advisingBankParty = proxy.wellKnownPartyFromX500Name(
                CordaX500Name.parse(String.valueOf(form.get("advisingBank")))
        );

        Party issuingBankParty = proxy.wellKnownPartyFromX500Name(
                CordaX500Name.parse(String.valueOf(form.get("issuingBank")))
        );

        try {
            SignedTransaction result = proxy.startFlowDynamic(
                    ApplyForLetterOfCreditFlow.Initiator.class,
                    form.get("purchaseOrderId"),
                    form.get("locType"),
                    form.get("locExpiryDate"),
                    advisingBankParty,
                    issuingBankParty,
                    Long.valueOf(form.get("locValue").toString()),
                    form.get("loadingPortAddress"),
                    form.get("loadingPortCity"),
                    form.get("loadingPortCountry"),
                    form.get("dischargePortAddress"),
                    form.get("dischargePortCity"),
                    form.get("dischargePortCountry")
            ).getReturnValue().get();
            // Return the response.
            final HashMap<String, Object> response = new HashMap<>();
            response.put("tx_id", result.getId());
            response.put("letter_of_credit", result.getTx().getOutput(0));
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception ex) {
            final HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to complete flow. " + ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }

    @PostMapping("/approve-loc")
    @ResponseBody
    public ResponseEntity approveLetterOfCredit(@RequestBody HashMap<String, Object> form) {
        try {
            SignedTransaction result = proxy.startFlowDynamic(
                    ApproveLetterOfCreditApplicationFlow.Initiator.class,
                    form.get("locId"),
                    form.get("locStatus")
            ).getReturnValue().get();
            // Return the response.
            final HashMap<String, Object> response = new HashMap<>();
            response.put("tx_id", result.getId());
            response.put("letter_of_credit", result.getTx().getOutput(0));
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(response);
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception ex) {
            final HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to complete flow. " + ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }

    @PostMapping("/ship-products")
    @ResponseBody
    public ResponseEntity shipProducts(@RequestBody HashMap<String, Object> form) {
        try {
            SignedTransaction result = proxy.startFlowDynamic(
                    ShipProductsFlow.Initiator.class,
                    form.get("locId"),
                    form.get("carrierCompanyName"),
                    form.get("carrierName"),
                    form.get("loadingDate"),
                    form.get("dischargeDate"),
                    form.get("productDescription")
            ).getReturnValue().get();
            // Return the response.
            final HashMap<String, Object> response = new HashMap<>();
            response.put("tx_id", result.getId());
            response.put("letter_of_credit", result.getTx().getOutput(0));
            response.put("bill_of_lading", result.getTx().getOutput(1));
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception ex) {
            final HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to complete flow. " + ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }

    @PostMapping("/pay-seller")
    @ResponseBody
    public ResponseEntity paySeller(@RequestBody HashMap<String, Object> form) {
        try {
            SignedTransaction result = proxy.startFlowDynamic(
                    PaySellerFlow.Initiator.class,
                    form.get("locId"),
                    form.get("billOfLadingId")
            ).getReturnValue().get();
            // Return the response.
            final HashMap<String, Object> response = new HashMap<>();
            response.put("tx_id", result.getId());
            response.put("letter_of_credit", result.getTx().getOutput(0));
            response.put("bill_of_lading", result.getTx().getOutput(1));
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(response);

            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception ex) {
            final HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to complete flow. " + ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }

    @PostMapping("/pay-advising-bank")
    @ResponseBody
    public ResponseEntity payAdvisingBank(@RequestBody HashMap<String, Object> form) {
        try {
            SignedTransaction result = proxy.startFlowDynamic(
                    PayAdvisingBankFlow.Initiator.class,
                    form.get("locId"),
                    form.get("billOfLadingId")
            ).getReturnValue().get();
            // Return the response.
            final HashMap<String, Object> response = new HashMap<>();
            response.put("tx_id", result.getId());
            response.put("letter_of_credit", result.getTx().getOutput(0));
            response.put("bill_of_lading", result.getTx().getOutput(1));
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(response);
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception ex) {
            final HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to complete flow. " + ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }

    @PostMapping("/pay-issuing-bank")
    @ResponseBody
    public ResponseEntity payIssuingBank(@RequestBody HashMap<String, Object> form) {
        try {
            SignedTransaction result = proxy.startFlowDynamic(
                    PayIssuingBankFlow.Initiator.class,
                    form.get("locId"),
                    form.get("billOfLadingId")
            ).getReturnValue().get();
            // Return the response.
            final HashMap<String, Object> response = new HashMap<>();
            response.put("tx_id", result.getId());
            response.put("letter_of_credit", result.getTx().getOutput(0));
            response.put("bill_of_lading", result.getTx().getOutput(1));
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(response);
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception ex) {
            final HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to complete flow. " + ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }


    // ---------------------------- Letter-Of-Credit ------------------------------------------------
}