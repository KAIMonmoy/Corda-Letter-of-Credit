package com.example.test.flow;

import com.example.flow.ShipProductsFlow;
import com.example.state.BillOfLadingState;
import com.example.state.LetterOfCreditState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.StartedMockNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;

public class ShipProductsFlowTests extends LetterOfCreditTests {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Throwable {
        final List<StateAndRef<LetterOfCreditState>> inputRefs =
                performApproveLetterOfCreditApplicationFlowWithIssued(network, buyer, seller, advisingBank, issuingBank);
        final LetterOfCreditState letterOfCredit = inputRefs.get(0).getState().getData();
        ShipProductsFlow.Initiator flow = new ShipProductsFlow.Initiator(
                letterOfCredit.getLocId(),
                demoBillOfLadingState.getBillOfLadingId(),
                demoBillOfLadingState.getCarrierCompanyName(),
                demoBillOfLadingState.getCarrierName(),
                demoBillOfLadingState.getLoadingDate(),
                demoBillOfLadingState.getDischargeDate(),
                demoBillOfLadingState.getProductDescription()
        );
        CordaFuture<SignedTransaction> future = seller.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(seller.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void transactionWithRejectedLetterOfCreditFails() throws Throwable {
        final List<StateAndRef<LetterOfCreditState>> inputRefs =
                performApproveLetterOfCreditApplicationFlowWithRejected(network, buyer, seller, advisingBank, issuingBank);
        final LetterOfCreditState letterOfCredit = inputRefs.get(0).getState().getData();
        ShipProductsFlow.Initiator flow = new ShipProductsFlow.Initiator(
                letterOfCredit.getLocId(),
                demoBillOfLadingState.getBillOfLadingId(),
                demoBillOfLadingState.getCarrierCompanyName(),
                demoBillOfLadingState.getCarrierName(),
                demoBillOfLadingState.getLoadingDate(),
                demoBillOfLadingState.getDischargeDate(),
                demoBillOfLadingState.getProductDescription()
        );
        CordaFuture<SignedTransaction> future = seller.startFlow(flow);
        network.runNetwork();

        exception.expectCause(instanceOf(FlowException.class));
        future.get();
    }

    @Test
    public void flowRecordsATransactionInAllPartiesTransactionStorage() throws Throwable {
        final List<StateAndRef<LetterOfCreditState>> inputRefs =
                performApproveLetterOfCreditApplicationFlowWithIssued(network, buyer, seller, advisingBank, issuingBank);
        final LetterOfCreditState letterOfCredit = inputRefs.get(0).getState().getData();
        ShipProductsFlow.Initiator flow = new ShipProductsFlow.Initiator(
                letterOfCredit.getLocId(),
                demoBillOfLadingState.getBillOfLadingId(),
                demoBillOfLadingState.getCarrierCompanyName(),
                demoBillOfLadingState.getCarrierName(),
                demoBillOfLadingState.getLoadingDate(),
                demoBillOfLadingState.getDischargeDate(),
                demoBillOfLadingState.getProductDescription()
        );
        CordaFuture<SignedTransaction> future = seller.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(seller, buyer, advisingBank, issuingBank)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void flowRecordsTheCorrectLOCInAllPartiesVaults() throws Throwable {
        final List<StateAndRef<LetterOfCreditState>> inputRefs =
                performApproveLetterOfCreditApplicationFlowWithIssued(network, buyer, seller, advisingBank, issuingBank);
        final LetterOfCreditState letterOfCredit = inputRefs.get(0).getState().getData();
        ShipProductsFlow.Initiator flow = new ShipProductsFlow.Initiator(
                letterOfCredit.getLocId(),
                demoBillOfLadingState.getBillOfLadingId(),
                demoBillOfLadingState.getCarrierCompanyName(),
                demoBillOfLadingState.getCarrierName(),
                demoBillOfLadingState.getLoadingDate(),
                demoBillOfLadingState.getDischargeDate(),
                demoBillOfLadingState.getProductDescription()
        );
        CordaFuture<SignedTransaction> future = seller.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        // We check the recorded transaction in all vaults.
        for (StartedMockNode node : ImmutableList.of(seller, buyer)) {
            node.transaction(() -> {
                List<StateAndRef<LetterOfCreditState>> letterOfCredits =
                        node.getServices().getVaultService().queryBy(LetterOfCreditState.class).getStates();
                assertEquals(1, letterOfCredits.size());
                LetterOfCreditState recordedLetterOfCredit = letterOfCredits.get(0).getState().getData();
                assertEquals(demoLetterOfCreditState.getLocId(), recordedLetterOfCredit.getLocId());
                assertEquals("SHIPPED", recordedLetterOfCredit.getLocStatus());

                List<StateAndRef<BillOfLadingState>> billsOfLading =
                        node.getServices().getVaultService().queryBy(BillOfLadingState.class).getStates();
                assertEquals(1, billsOfLading.size());
                BillOfLadingState billOfLading = billsOfLading.get(0).getState().getData();
                assertEquals(demoBillOfLadingState.getBillOfLadingId(), billOfLading.getBillOfLadingId());
                assertEquals(demoBillOfLadingState.getSeller(), billOfLading.getCurrentOwner());
                assertEquals(demoBillOfLadingState.getSeller(), billOfLading.getSeller());
                assertEquals(demoBillOfLadingState.getBuyer(), billOfLading.getBuyer());
                assertEquals(demoBillOfLadingState.getAdvisingBank(), billOfLading.getAdvisingBank());
                assertEquals(demoBillOfLadingState.getIssuingBank(), billOfLading.getIssuingBank());
                assertEquals(demoBillOfLadingState.getCarrierCompanyName(), billOfLading.getCarrierCompanyName());
                assertEquals(demoBillOfLadingState.getCarrierName(), billOfLading.getCarrierName());
                assertEquals(demoBillOfLadingState.getLoadingDate(), billOfLading.getLoadingDate());
                assertEquals(demoBillOfLadingState.getDischargeDate(), billOfLading.getDischargeDate());
                assertEquals(demoBillOfLadingState.getProductName(), billOfLading.getProductName());
                assertEquals(demoBillOfLadingState.getProductDescription(), billOfLading.getProductDescription());
                assertEquals(demoBillOfLadingState.getProductQuantity(), billOfLading.getProductQuantity());
                assertEquals(demoBillOfLadingState.getProductPriceInUSD(), billOfLading.getProductPriceInUSD());
                assertEquals(demoBillOfLadingState.getProductGrossWeightInKG(), billOfLading.getProductGrossWeightInKG());
                assertEquals(demoBillOfLadingState.getLoadingPortAddress(), billOfLading.getLoadingPortAddress());
                assertEquals(demoBillOfLadingState.getLoadingPortCity(), billOfLading.getLoadingPortCity());
                assertEquals(demoBillOfLadingState.getLoadingPortCountry(), billOfLading.getLoadingPortCountry());
                assertEquals(demoBillOfLadingState.getDischargePortAddress(), billOfLading.getDischargePortAddress());
                assertEquals(demoBillOfLadingState.getDischargePortCity(), billOfLading.getDischargePortCity());
                assertEquals(demoBillOfLadingState.getDischargePortCountry(), billOfLading.getDischargePortCountry());
                return null;
            });
        }
    }
}
