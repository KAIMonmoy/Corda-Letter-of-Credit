package com.example.test.flow;

import com.example.flow.ApproveLetterOfCreditApplicationFlow;
import com.example.state.LetterOfCreditState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.StartedMockNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ApproveLetterOfCreditApplicationFlowTests extends LetterOfCreditTests {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiatorOnIssued() throws Throwable {
        final List<StateAndRef<LetterOfCreditState>> inputRefs =
                performApplyForLetterOfCreditFlow(network, buyer, seller, advisingBank, issuingBank);
        final LetterOfCreditState letterOfCreditState = inputRefs.get(0).getState().getData();
        ApproveLetterOfCreditApplicationFlow.Initiator flow = new ApproveLetterOfCreditApplicationFlow.Initiator(
                letterOfCreditState.getLocId(),
                "ISSUED"
        );
        CordaFuture<SignedTransaction> future = issuingBank.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(issuingBank.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiatorOnRejected() throws Throwable {
        final List<StateAndRef<LetterOfCreditState>> inputRefs =
                performApplyForLetterOfCreditFlow(network, buyer, seller, advisingBank, issuingBank);
        final LetterOfCreditState letterOfCreditState = inputRefs.get(0).getState().getData();
        ApproveLetterOfCreditApplicationFlow.Initiator flow = new ApproveLetterOfCreditApplicationFlow.Initiator(
                letterOfCreditState.getLocId(),
                "REJECTED"
        );
        CordaFuture<SignedTransaction> future = issuingBank.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(issuingBank.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInAllPartiesTransactionStorageOnIssued() throws Throwable {
        final List<StateAndRef<LetterOfCreditState>> inputRefs =
                performApplyForLetterOfCreditFlow(network, buyer, seller, advisingBank, issuingBank);
        final LetterOfCreditState letterOfCreditState = inputRefs.get(0).getState().getData();
        ApproveLetterOfCreditApplicationFlow.Initiator flow = new ApproveLetterOfCreditApplicationFlow.Initiator(
                letterOfCreditState.getLocId(),
                "ISSUED"
        );
        CordaFuture<SignedTransaction> future = issuingBank.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(seller, buyer, advisingBank, issuingBank)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void flowRecordsATransactionInAllPartiesTransactionStorageOnRejected() throws Throwable {
        final List<StateAndRef<LetterOfCreditState>> inputRefs =
                performApplyForLetterOfCreditFlow(network, buyer, seller, advisingBank, issuingBank);
        final LetterOfCreditState letterOfCreditState = inputRefs.get(0).getState().getData();
        ApproveLetterOfCreditApplicationFlow.Initiator flow = new ApproveLetterOfCreditApplicationFlow.Initiator(
                letterOfCreditState.getLocId(),
                "REJECTED"
        );
        CordaFuture<SignedTransaction> future = issuingBank.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(seller, buyer, advisingBank, issuingBank)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void flowRecordsTheCorrectLOCInAllPartiesVaultsOnIssued() throws Throwable {
        final List<StateAndRef<LetterOfCreditState>> inputRefs =
                performApplyForLetterOfCreditFlow(network, buyer, seller, advisingBank, issuingBank);
        final LetterOfCreditState letterOfCreditState = inputRefs.get(0).getState().getData();
        ApproveLetterOfCreditApplicationFlow.Initiator flow = new ApproveLetterOfCreditApplicationFlow.Initiator(
                letterOfCreditState.getLocId(),
                "ISSUED"
        );
        CordaFuture<SignedTransaction> future = issuingBank.startFlow(flow);
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
                assertEquals(demoLetterOfCreditState.getLocType(), recordedLetterOfCredit.getLocType());
                assertEquals(demoLetterOfCreditState.getLocExpiryDate(), recordedLetterOfCredit.getLocExpiryDate());
                assertEquals(demoLetterOfCreditState.getSeller(), recordedLetterOfCredit.getSeller());
                assertEquals(demoLetterOfCreditState.getBuyer(), recordedLetterOfCredit.getBuyer());
                assertEquals(demoLetterOfCreditState.getAdvisingBank(), recordedLetterOfCredit.getAdvisingBank());
                assertEquals(demoLetterOfCreditState.getIssuingBank(), recordedLetterOfCredit.getIssuingBank());
                assertEquals(demoLetterOfCreditState.getLocValue(), recordedLetterOfCredit.getLocValue());
                assertEquals(demoLetterOfCreditState.getLoadingPortAddress(), recordedLetterOfCredit.getLoadingPortAddress());
                assertEquals(demoLetterOfCreditState.getLoadingPortCity(), recordedLetterOfCredit.getLoadingPortCity());
                assertEquals(demoLetterOfCreditState.getLoadingPortCountry(), recordedLetterOfCredit.getLoadingPortCountry());
                assertEquals(demoLetterOfCreditState.getDischargePortAddress(), recordedLetterOfCredit.getDischargePortAddress());
                assertEquals(demoLetterOfCreditState.getDischargePortCity(), recordedLetterOfCredit.getDischargePortCity());
                assertEquals(demoLetterOfCreditState.getDischargePortCountry(), recordedLetterOfCredit.getDischargePortCountry());
                assertEquals(demoLetterOfCreditState.getProductName(), recordedLetterOfCredit.getProductName());
                assertEquals(demoLetterOfCreditState.getProductQuantity(), recordedLetterOfCredit.getProductQuantity());
                assertEquals(demoLetterOfCreditState.getProductPriceInUSD(), recordedLetterOfCredit.getProductPriceInUSD());
                assertEquals(demoLetterOfCreditState.getProductGrossWeightInKG(), recordedLetterOfCredit.getProductGrossWeightInKG());
                assertEquals("ISSUED", recordedLetterOfCredit.getLocStatus());
                return null;
            });
        }
    }

    @Test
    public void flowRecordsTheCorrectLOCInAllPartiesVaultsOnRejected() throws Throwable {
        final List<StateAndRef<LetterOfCreditState>> inputRefs =
                performApplyForLetterOfCreditFlow(network, buyer, seller, advisingBank, issuingBank);
        final LetterOfCreditState letterOfCreditState = inputRefs.get(0).getState().getData();
        ApproveLetterOfCreditApplicationFlow.Initiator flow = new ApproveLetterOfCreditApplicationFlow.Initiator(
                letterOfCreditState.getLocId(),
                "REJECTED"
        );
        CordaFuture<SignedTransaction> future = issuingBank.startFlow(flow);
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
                assertEquals(demoLetterOfCreditState.getLocType(), recordedLetterOfCredit.getLocType());
                assertEquals(demoLetterOfCreditState.getLocExpiryDate(), recordedLetterOfCredit.getLocExpiryDate());
                assertEquals(demoLetterOfCreditState.getSeller(), recordedLetterOfCredit.getSeller());
                assertEquals(demoLetterOfCreditState.getBuyer(), recordedLetterOfCredit.getBuyer());
                assertEquals(demoLetterOfCreditState.getAdvisingBank(), recordedLetterOfCredit.getAdvisingBank());
                assertEquals(demoLetterOfCreditState.getIssuingBank(), recordedLetterOfCredit.getIssuingBank());
                assertEquals(demoLetterOfCreditState.getLocValue(), recordedLetterOfCredit.getLocValue());
                assertEquals(demoLetterOfCreditState.getLoadingPortAddress(), recordedLetterOfCredit.getLoadingPortAddress());
                assertEquals(demoLetterOfCreditState.getLoadingPortCity(), recordedLetterOfCredit.getLoadingPortCity());
                assertEquals(demoLetterOfCreditState.getLoadingPortCountry(), recordedLetterOfCredit.getLoadingPortCountry());
                assertEquals(demoLetterOfCreditState.getDischargePortAddress(), recordedLetterOfCredit.getDischargePortAddress());
                assertEquals(demoLetterOfCreditState.getDischargePortCity(), recordedLetterOfCredit.getDischargePortCity());
                assertEquals(demoLetterOfCreditState.getDischargePortCountry(), recordedLetterOfCredit.getDischargePortCountry());
                assertEquals(demoLetterOfCreditState.getProductName(), recordedLetterOfCredit.getProductName());
                assertEquals(demoLetterOfCreditState.getProductQuantity(), recordedLetterOfCredit.getProductQuantity());
                assertEquals(demoLetterOfCreditState.getProductPriceInUSD(), recordedLetterOfCredit.getProductPriceInUSD());
                assertEquals(demoLetterOfCreditState.getProductGrossWeightInKG(), recordedLetterOfCredit.getProductGrossWeightInKG());
                assertEquals("REJECTED", recordedLetterOfCredit.getLocStatus());
                return null;
            });
        }
    }
}
