package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.LetterOfCreditContract;
import com.example.state.LetterOfCreditState;
import com.example.state.PurchaseOrderState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.contract.LetterOfCreditContract.Commands.ApplyForLetterOfCredit;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public interface ApplyForLetterOfCreditFlow {
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

        @NotNull
        private final ProgressTracker progressTracker = new ProgressTracker(
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
        @NotNull private final String locId;
        @NotNull private final String locType;
        @NotNull private final String locExpiryDate;
        @NotNull private final Party advisingBank;
        @NotNull private final Party issuingBank;
        @NotNull private final Long locValue;
        @NotNull private final String loadingPortAddress;
        @NotNull private final String loadingPortCity;
        @NotNull private final String loadingPortCountry;
        @NotNull private final String dischargePortAddress;
        @NotNull private final String dischargePortCity;
        @NotNull private final String dischargePortCountry;

        public Initiator(@NotNull String purchaseOrderId,
                         @NotNull String locId,
                         @NotNull String locType,
                         @NotNull String locExpiryDate,
                         @NotNull Party advisingBank,
                         @NotNull Party issuingBank,
                         @NotNull Long locValue,
                         @NotNull String loadingPortAddress,
                         @NotNull String loadingPortCity,
                         @NotNull String loadingPortCountry,
                         @NotNull String dischargePortAddress,
                         @NotNull String dischargePortCity,
                         @NotNull String dischargePortCountry) {
            this.purchaseOrderId = purchaseOrderId;
            this.locId = locId;
            this.locType = locType;
            this.locExpiryDate = locExpiryDate;
            this.advisingBank = advisingBank;
            this.issuingBank = issuingBank;
            this.locValue = locValue;
            this.loadingPortAddress = loadingPortAddress;
            this.loadingPortCity = loadingPortCity;
            this.loadingPortCountry = loadingPortCountry;
            this.dischargePortAddress = dischargePortAddress;
            this.dischargePortCity = dischargePortCity;
            this.dischargePortCountry = dischargePortCountry;
        }



        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // Taking first notary on network. (For Dev)
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Stage-1: GENERATING_TRANSACTION
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            StateAndRef<PurchaseOrderState> inputPurchaseOrderRef = getPurchaseOrderStateAndRef(purchaseOrderId);
            final PurchaseOrderState inputPurchaseOrder =  inputPurchaseOrderRef.getState().getData();
            if (!inputPurchaseOrderRef.getState().getData().getBuyer().equals(getOurIdentity()))
                throw new FlowException("I (" + getOurIdentity() + ") must be the buyer in the referenced PurchaseOrderState.");
            if (!isLOCIdUnique(locId))
                throw new FlowException("locId:" + locId + " already exists.");
            final LetterOfCreditState letterOfCredit = new LetterOfCreditState(
                    locId,
                    locType,
                    locExpiryDate,
                    inputPurchaseOrder.getSeller(),
                    inputPurchaseOrder.getBuyer(),
                    advisingBank,
                    issuingBank,
                    locValue,
                    loadingPortAddress,
                    loadingPortCity,
                    loadingPortCountry,
                    dischargePortAddress,
                    dischargePortCity,
                    dischargePortCountry,
                    inputPurchaseOrder.getProductName(),
                    inputPurchaseOrder.getProductQuantity(),
                    inputPurchaseOrder.getProductPriceInUSD(),
                    inputPurchaseOrder.getProductGrossWeightInKG(),
                    inputPurchaseOrder.getPurchaseOrderId()
            );
            final List<Party> requiredSigners = Arrays.asList(
                    inputPurchaseOrder.getBuyer(),
                    inputPurchaseOrder.getSeller(),
                    issuingBank,
                    advisingBank
            );
            Command<ApplyForLetterOfCredit> txCommand = new Command<>(
                    new ApplyForLetterOfCredit(),
                    requiredSigners.stream().map(Party::getOwningKey).collect(Collectors.toList())
            );
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputPurchaseOrderRef)
                    .addOutputState(letterOfCredit, LetterOfCreditContract.LOC_CONTRACT_ID)
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

        boolean isLOCIdUnique(String locId) {
            Vault.Page<LetterOfCreditState> results =
                    getServiceHub().getVaultService().queryBy(LetterOfCreditState.class);
            return results.getStates().stream().noneMatch(
                    it -> it.getState().getData().getLocId().equals(locId)
            );
        }

        StateAndRef<PurchaseOrderState> getPurchaseOrderStateAndRef(String purchaseOrderId) throws FlowException {
            QueryCriteria.VaultQueryCriteria criteria =
                    new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            Vault.Page<PurchaseOrderState> results =
                    getServiceHub().getVaultService().queryBy(PurchaseOrderState.class, criteria);
            List<StateAndRef<PurchaseOrderState>> purchaseOrders = results.getStates();
            for (StateAndRef<PurchaseOrderState> order : purchaseOrders) {
                if (order.getState().getData().getPurchaseOrderId().equals(purchaseOrderId))
                    return order;
            }
            throw new FlowException("Unconsumed PurchaseOrderState with ID:" + purchaseOrderId + "not found!");
        }
    }

    @InitiatedBy(ApplyForLetterOfCreditFlow.Initiator.class)
    class Responder extends FlowLogic<SignedTransaction> {

        private final FlowSession buyerSession;

        public Responder(FlowSession buyerSession) { this.buyerSession = buyerSession; }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private final Party ourId;
                private SignTxFlow(
                        FlowSession otherPartyFlow,
                        ProgressTracker progressTracker,
                        Party ourId
                ) {
                    super(otherPartyFlow, progressTracker);
                    this.ourId = ourId;
                }

                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) {
                    requireThat(requirements -> {
                        final List<LetterOfCreditState> letterOfCreditStates =
                                stx.getTx().outputsOfType(LetterOfCreditState.class);

                        requirements.using(
                                "There must be exactly 1 LetterOfCreditState in output.",
                                letterOfCreditStates.size() == 1
                        );

                        final LetterOfCreditState proposedLetterOfCreditState = letterOfCreditStates.get(0);

                        if (proposedLetterOfCreditState.getSeller().equals(ourId)) {
                            requirements.using(
                                    "LOC Value must be positive and sufficient for seller.",
                                    proposedLetterOfCreditState.getLocValue() > 0 &&
                                            proposedLetterOfCreditState.getLocValue() >=
                                                    proposedLetterOfCreditState.getProductQuantity() * proposedLetterOfCreditState.getProductPriceInUSD()
                            );
                        } else {
                            requirements.using(
                                    "I (" + getOurIdentity() + ") must be relevant.",
                                    proposedLetterOfCreditState.getIssuingBank().equals(ourId)
                                    || proposedLetterOfCreditState.getAdvisingBank().equals(ourId)
                            );
                        }

                        return null;
                    });
                }
            }
            final SignTxFlow signTxFlow = new SignTxFlow(buyerSession,
                    SignTransactionFlow.Companion.tracker(),
                    getOurIdentity());
            final SecureHash txId = subFlow(signTxFlow).getId();

            return subFlow(new ReceiveFinalityFlow(buyerSession, txId));
        }
    }
}
