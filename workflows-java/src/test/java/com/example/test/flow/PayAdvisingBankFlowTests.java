package com.example.test.flow;

import com.example.flow.PayAdvisingBankFlow;
import com.example.state.BillOfLadingState;
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

public class PayAdvisingBankFlowTests extends LetterOfCreditTests {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Throwable {
        final List<StateAndRef> inputRefs =
                performPaySellerFlow(network, buyer, seller, advisingBank, issuingBank);
        final LetterOfCreditState inputLOC = (LetterOfCreditState) inputRefs.get(0).getState().getData();
        final BillOfLadingState inputBillOfLading = (BillOfLadingState) inputRefs.get(1).getState().getData();
        PayAdvisingBankFlow.Initiator flow = new PayAdvisingBankFlow.Initiator(
                inputLOC.getLocId(),
                inputBillOfLading.getBillOfLadingId()
        );
        CordaFuture<SignedTransaction> future = issuingBank.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(issuingBank.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInAllPartiesTransactionStorage() throws Throwable {
        final List<StateAndRef> inputRefs =
                performPaySellerFlow(network, buyer, seller, advisingBank, issuingBank);
        final LetterOfCreditState inputLOC = (LetterOfCreditState) inputRefs.get(0).getState().getData();
        final BillOfLadingState inputBillOfLading = (BillOfLadingState) inputRefs.get(1).getState().getData();
        PayAdvisingBankFlow.Initiator flow = new PayAdvisingBankFlow.Initiator(
                inputLOC.getLocId(),
                inputBillOfLading.getBillOfLadingId()
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
    public void flowRecordsTheCorrectStatesInAllPartiesVaults() throws Throwable {
        final List<StateAndRef> inputRefs =
                performPaySellerFlow(network, buyer, seller, advisingBank, issuingBank);
        final LetterOfCreditState inputLOC = (LetterOfCreditState) inputRefs.get(0).getState().getData();
        final BillOfLadingState inputBillOfLading = (BillOfLadingState) inputRefs.get(1).getState().getData();
        PayAdvisingBankFlow.Initiator flow = new PayAdvisingBankFlow.Initiator(
                inputLOC.getLocId(),
                inputBillOfLading.getBillOfLadingId()
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
                assertEquals("ADVISING_BANK_PAID", recordedLetterOfCredit.getLocStatus());

                List<StateAndRef<BillOfLadingState>> bills =
                        node.getServices().getVaultService().queryBy(BillOfLadingState.class).getStates();
                assertEquals(1, bills.size());
                BillOfLadingState recordedBill = bills.get(0).getState().getData();
                assertEquals(recordedLetterOfCredit.getIssuingBank(), recordedBill.getCurrentOwner());

                return null;
            });
        }
    }
}
