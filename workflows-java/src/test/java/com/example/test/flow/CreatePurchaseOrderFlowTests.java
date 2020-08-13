package com.example.test.flow;

import com.example.flow.CreatePurchaseOrderFlow;
import com.example.state.PurchaseOrderState;
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

public class CreatePurchaseOrderFlowTests extends LetterOfCreditTests {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        CreatePurchaseOrderFlow.Initiator flow = new CreatePurchaseOrderFlow.Initiator(
                demoPurchaseOrder.getBuyer(),
                demoPurchaseOrder.getPurchaseOrderIssueDate(),
                demoPurchaseOrder.getProductName(),
                demoPurchaseOrder.getProductQuantity(),
                demoPurchaseOrder.getProductPriceInUSD(),
                demoBillOfLadingState.getProductGrossWeightInKG()
        );
        CordaFuture<SignedTransaction> future = seller.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        System.out.println("Okay, here!");
        signedTx.verifySignaturesExcept(seller.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheBuyer() throws Exception {
        CreatePurchaseOrderFlow.Initiator flow = new CreatePurchaseOrderFlow.Initiator(
                demoPurchaseOrder.getBuyer(),
                demoPurchaseOrder.getPurchaseOrderIssueDate(),
                demoPurchaseOrder.getProductName(),
                demoPurchaseOrder.getProductQuantity(),
                demoPurchaseOrder.getProductPriceInUSD(),
                demoBillOfLadingState.getProductGrossWeightInKG()
        );
        CordaFuture<SignedTransaction> future = seller.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        System.out.println("Okay, here!");
        signedTx.verifySignaturesExcept(buyer.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorage() throws Exception {
        CreatePurchaseOrderFlow.Initiator flow = new CreatePurchaseOrderFlow.Initiator(
                demoPurchaseOrder.getBuyer(),
                demoPurchaseOrder.getPurchaseOrderIssueDate(),
                demoPurchaseOrder.getProductName(),
                demoPurchaseOrder.getProductQuantity(),
                demoPurchaseOrder.getProductPriceInUSD(),
                demoBillOfLadingState.getProductGrossWeightInKG()
        );
        CordaFuture<SignedTransaction> future = seller.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(seller, buyer)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void flowRecordsTheCorrectPurchaseOrderInBothPartiesVaults() {
        CreatePurchaseOrderFlow.Initiator flow = new CreatePurchaseOrderFlow.Initiator(
                demoPurchaseOrder.getBuyer(),
                demoPurchaseOrder.getPurchaseOrderIssueDate(),
                demoPurchaseOrder.getProductName(),
                demoPurchaseOrder.getProductQuantity(),
                demoPurchaseOrder.getProductPriceInUSD(),
                demoBillOfLadingState.getProductGrossWeightInKG()
        );
        seller.startFlow(flow);
        network.runNetwork();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(seller, buyer)) {
            node.transaction(() -> {
                List<StateAndRef<PurchaseOrderState>> purchaseOrders =
                        node.getServices().getVaultService().queryBy(PurchaseOrderState.class).getStates();
                assertEquals(1, purchaseOrders.size());
                PurchaseOrderState recordedPurchaseOrder = purchaseOrders.get(0).getState().getData();
                assertEquals(demoPurchaseOrder.getSeller(), recordedPurchaseOrder.getSeller());
                assertEquals(demoPurchaseOrder.getBuyer(), recordedPurchaseOrder.getBuyer());
                assertEquals(demoPurchaseOrder.getPurchaseOrderIssueDate(), recordedPurchaseOrder.getPurchaseOrderIssueDate());
                assertEquals(demoPurchaseOrder.getProductName(), recordedPurchaseOrder.getProductName());
                assertEquals(demoPurchaseOrder.getProductQuantity(), recordedPurchaseOrder.getProductQuantity());
                assertEquals(demoPurchaseOrder.getProductPriceInUSD(), recordedPurchaseOrder.getProductPriceInUSD());
                assertEquals(demoBillOfLadingState.getProductGrossWeightInKG(), recordedPurchaseOrder.getProductGrossWeightInKG());
                return null;
            });
        }
    }
}
