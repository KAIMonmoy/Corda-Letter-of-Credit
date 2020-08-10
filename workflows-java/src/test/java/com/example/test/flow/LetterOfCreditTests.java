package com.example.test.flow;

import com.example.flow.ApproveLetterOfCreditApplicationFlow;
import com.example.flow.ApplyForLetterOfCreditFlow;
import com.example.flow.CreatePurchaseOrderFlow;
import com.example.state.BillOfLadingState;
import com.example.state.LetterOfCreditState;
import com.example.state.PurchaseOrderState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;

import java.util.List;

abstract class LetterOfCreditTests {
    public MockNetwork network;
    public StartedMockNode buyer;
    public StartedMockNode seller;
    public StartedMockNode issuingBank;
    public StartedMockNode advisingBank;

    public static PurchaseOrderState demoPurchaseOrder;
    public static LetterOfCreditState demoLetterOfCreditState;
    public static BillOfLadingState demoBillOfLadingState;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.example.contract"),
                TestCordapp.findCordapp("com.example.flow"))));
        buyer = network.createPartyNode(new CordaX500Name("Buyer", "Kowloon", "HK"));
        seller = network.createPartyNode(new CordaX500Name("Seller", "Chittagong", "BD"));
        advisingBank = network.createPartyNode(new CordaX500Name("AdvisingBank", "Dhaka", "BD"));
        issuingBank = network.createPartyNode(new CordaX500Name("IssuingBank", "Kowloon", "HK"));
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        for (StartedMockNode node : ImmutableList.of(buyer, seller, issuingBank, advisingBank)) {
            node.registerInitiatedFlow(CreatePurchaseOrderFlow.Initiator.class, CreatePurchaseOrderFlow.Responder.class);
            node.registerInitiatedFlow(ApplyForLetterOfCreditFlow.Initiator.class, ApplyForLetterOfCreditFlow.Responder.class);
            node.registerInitiatedFlow(ApproveLetterOfCreditApplicationFlow.Initiator.class, ApproveLetterOfCreditApplicationFlow.Responder.class);
        }

        demoPurchaseOrder = new PurchaseOrderState(
                "1",
                seller.getInfo().getLegalIdentities().get(0),
                buyer.getInfo().getLegalIdentities().get(0),
                "01-01-2020",
                "product",
                100L,
                5L,
                700L
        );
        demoLetterOfCreditState = new LetterOfCreditState(
                "1",
                "A",
                "31-01-2020",
                demoPurchaseOrder.getSeller(),
                demoPurchaseOrder.getBuyer(),
                advisingBank.getInfo().getLegalIdentities().get(0),
                issuingBank.getInfo().getLegalIdentities().get(0),
                500L,
                "CTG Port",
                "Chittagong",
                "BD",
                "KWL Port",
                "Kowloon",
                "HK",
                demoPurchaseOrder.getProductName(),
                demoPurchaseOrder.getProductQuantity(),
                demoPurchaseOrder.getProductPriceInUSD(),
                demoPurchaseOrder.getProductGrossWeightInKG()
        );
        demoBillOfLadingState = new BillOfLadingState(
                "1",
                demoLetterOfCreditState.getSeller(),
                demoLetterOfCreditState.getBuyer(),
                "CTG Ships Ltd",
                "CTG Ship",
                "02-01-2020",
                "10-01-2020",
                demoLetterOfCreditState.getProductName(),
                "No Damage",
                demoLetterOfCreditState.getProductQuantity(),
                demoLetterOfCreditState.getProductPriceInUSD(),
                demoLetterOfCreditState.getProductGrossWeightInKG(),
                "CTG Port",
                "Chittagong",
                "BD",
                "KWL Port",
                "Kowloon",
                "HK"
        );

        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @NotNull
    public static List<StateAndRef<PurchaseOrderState>> performCreatePurchaseOrderFlow(
            @NotNull final MockNetwork network,
            @NotNull final StartedMockNode buyer,
            @NotNull final StartedMockNode seller
    ) throws Throwable {
        CreatePurchaseOrderFlow.Initiator flow = new CreatePurchaseOrderFlow.Initiator(
                demoPurchaseOrder.getPurchaseOrderId(),
                buyer.getInfo().getLegalIdentities().get(0),
                demoPurchaseOrder.getPurchaseOrderIssueDate(),
                demoPurchaseOrder.getProductName(),
                demoPurchaseOrder.getProductQuantity(),
                demoPurchaseOrder.getProductPriceInUSD(),
                demoBillOfLadingState.getProductGrossWeightInKG()
        );
        CordaFuture<SignedTransaction> future = seller.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        return signedTx.toLedgerTransaction(seller.getServices())
                .outRefsOfType(PurchaseOrderState.class);
    }

    @NotNull
    public static List<StateAndRef<LetterOfCreditState>> performApplyForLetterOfCreditFlow(
            @NotNull final MockNetwork network,
            @NotNull final StartedMockNode buyer,
            @NotNull final StartedMockNode seller,
            @NotNull final StartedMockNode advisingBank,
            @NotNull final StartedMockNode issuingBank
    ) throws Throwable {
        final List<StateAndRef<PurchaseOrderState>> inputRefs =
                performCreatePurchaseOrderFlow(network, buyer, seller);
        final PurchaseOrderState purchaseOrder = inputRefs.get(0).getState().getData();
        ApplyForLetterOfCreditFlow.Initiator flow = new ApplyForLetterOfCreditFlow.Initiator(
                purchaseOrder.getPurchaseOrderId(),
                demoLetterOfCreditState.getLocId(),
                demoLetterOfCreditState.getLocType(),
                demoLetterOfCreditState.getLocExpiryDate(),
                advisingBank.getInfo().getLegalIdentities().get(0),
                issuingBank.getInfo().getLegalIdentities().get(0),
                demoLetterOfCreditState.getLocValue(),
                demoLetterOfCreditState.getLoadingPortAddress(),
                demoLetterOfCreditState.getLoadingPortCity(),
                demoLetterOfCreditState.getLoadingPortCountry(),
                demoLetterOfCreditState.getDischargePortAddress(),
                demoLetterOfCreditState.getDischargePortCity(),
                demoLetterOfCreditState.getDischargePortCountry()
        );
        CordaFuture<SignedTransaction> future = buyer.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        return signedTx.toLedgerTransaction(seller.getServices())
                .outRefsOfType(LetterOfCreditState.class);
    }

}
