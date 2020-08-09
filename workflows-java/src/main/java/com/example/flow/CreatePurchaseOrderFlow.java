package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.LetterOfCreditContract;
import com.example.state.PurchaseOrderState;
import net.corda.core.contracts.Command;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.contract.LetterOfCreditContract.Commands.CreatePurchaseOrder;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public interface CreatePurchaseOrderFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator extends FlowLogic<SignedTransaction> {

        private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction.");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        private final ProgressTracker.Step GATHERING_SIGNS = new ProgressTracker.Step("Gathering signatures.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and finalizing transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        @NotNull private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGNS,
                FINALISING_TRANSACTION
        );

        @NotNull
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @NotNull private final String purchaseOrderId;
        @NotNull private final Party seller = getOurIdentity();
        @NotNull private final Party buyer;
        @NotNull private final String purchaseOrderIssueDate;
        @NotNull private final String productName;
        @NotNull private final Long productQuantity;
        @NotNull private final Long productPriceInUSD;
        @NotNull private final Long productGrossWeightInKG;

        public Initiator(
                @NotNull String purchaseOrderId,
                @NotNull Party buyer,
                @NotNull String purchaseOrderIssueDate,
                @NotNull String productName,
                @NotNull Long productQuantity,
                @NotNull Long productPriceInUSD,
                @NotNull Long productGrossWeightInKG) {
            this.purchaseOrderId = purchaseOrderId;
            this.buyer = buyer;
            this.purchaseOrderIssueDate = purchaseOrderIssueDate;
            this.productName = productName;
            this.productQuantity = productQuantity;
            this.productPriceInUSD = productPriceInUSD;
            this.productGrossWeightInKG = productGrossWeightInKG;
        }

        @Override
        public SignedTransaction call() throws FlowException {
            // Taking first notary on network. (For Dev)
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Stage-1: GENERATING_TRANSACTION
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            if (!isPurchaseOrderIdUnique(purchaseOrderId))
                throw new FlowException("purchaseOrderId:" + purchaseOrderId + " already exists.");
            final PurchaseOrderState purchaseOrder = new PurchaseOrderState(
                    purchaseOrderId,
                    seller,
                    buyer,
                    purchaseOrderIssueDate,
                    productName,
                    productQuantity,
                    productPriceInUSD,
                    productGrossWeightInKG
            );
            final List<Party> requiredSigners = Arrays.asList(
                    seller,
                    buyer
            );
            Command<CreatePurchaseOrder> txCommand = new Command<>(
                    new CreatePurchaseOrder(),
                    requiredSigners.stream().map(Party::getOwningKey).collect(Collectors.toList())
            );
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(purchaseOrder, LetterOfCreditContract.LOC_CONTRACT_ID)
                    .addCommand(txCommand);

            // Stage-2: VERIFYING_TRANSACTION
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            txBuilder.verify(getServiceHub());

            // Stage-3: SIGNING_TRANSACTION
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            final SignedTransaction partlySignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Stage-4: GATHERING_SIGNS
            progressTracker.setCurrentStep(GATHERING_SIGNS);
            List<FlowSession> signerFlows = requiredSigners.stream()
                    .filter(it -> !it.equals(getOurIdentity()))
                    .map(this::initiateFlow)
                    .collect(Collectors.toList());
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(
                            partlySignedTx,
                            signerFlows,
                            GATHERING_SIGNS.childProgressTracker()
                    )
            );

            // Stage-5: FINALISING_TRANSACTION
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(
                    new FinalityFlow(
                            fullySignedTx,
                            signerFlows,
                            FINALISING_TRANSACTION.childProgressTracker()
                    )
            );
        }

        boolean isPurchaseOrderIdUnique(String purchaseOrderId) {
            Vault.Page<PurchaseOrderState> results =
                    getServiceHub().getVaultService().queryBy(PurchaseOrderState.class);
            return results.getStates().stream().noneMatch(
                    it -> it.getState().getData().getPurchaseOrderId().equals(purchaseOrderId)
            );
        }
    }

    @InitiatedBy(Initiator.class)
    class Responder extends FlowLogic<SignedTransaction> {

        private final FlowSession sellerSession;

        public Responder(FlowSession sellerSession) { this.sellerSession = sellerSession; }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) {
                    requireThat(requirements -> {
                        final List<PurchaseOrderState> proposedPurchaseOrder =
                                stx.getTx().outputsOfType(PurchaseOrderState.class);

                        requirements.using(
                                "There must be exactly 1 PurchaseOrderState in output.",
                                proposedPurchaseOrder.size() == 1
                        );

                        requirements.using(
                                "I (" + getOurIdentity() + ") must be the buyer.",
                                proposedPurchaseOrder.get(0).getBuyer().equals(getOurIdentity())
                        );

                        return null;
                    });
                }
            }
            final SignTxFlow signTxFlow = new SignTxFlow(sellerSession, SignTransactionFlow.Companion.tracker());
            final SecureHash txId = subFlow(signTxFlow).getId();

            return subFlow(new ReceiveFinalityFlow(sellerSession, txId));
        }
    }
}
