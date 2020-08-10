package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.LetterOfCreditContract;
import com.example.state.BillOfLadingState;
import com.example.state.LetterOfCreditState;
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

import static com.example.contract.LetterOfCreditContract.Commands.PayIssuingBank;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public interface PayIssuingBankFlow {
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

        @NotNull private final String locId;
        @NotNull private final String billOfLadingId;

        public Initiator(@NotNull String locId, @NotNull String billOfLadingId) {
            this.locId = locId;
            this.billOfLadingId = billOfLadingId;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // Taking first notary on network. (For Dev)
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Stage-1: GENERATING_TRANSACTION
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            StateAndRef<LetterOfCreditState> inputLetterOfCreditRef = getLetterOfCreditStateAndRef(locId);
            final LetterOfCreditState inputLetterOfCredit =  inputLetterOfCreditRef.getState().getData();
            StateAndRef<BillOfLadingState> inputBillOfLadingRef = getBillOfLadingStateAndRef(billOfLadingId);
            final BillOfLadingState inputBillOfLading = inputBillOfLadingRef.getState().getData();
            if (!inputLetterOfCreditRef.getState().getData().getBuyer().equals(getOurIdentity()))
                throw new FlowException("I (" + getOurIdentity() + ") must be the buyer in the referenced LOC.");
            if (!inputLetterOfCredit.getLocStatus().equals("ADVISING_BANK_PAID"))
                throw new FlowException("Invalid locStatus:" + inputLetterOfCredit.getLocStatus() + " found. Required locStatus: APPLIED");
            final LetterOfCreditState letterOfCredit = LetterOfCreditState.locWithUpdatedStatus(
                    inputLetterOfCredit,
                    "ISSUING_BANK_PAID"
            );
            final BillOfLadingState billOfLadingState = BillOfLadingState.billOfLadingStateWithUpdatedOwner(
                    inputBillOfLading,
                    inputBillOfLading.getBuyer()
            );
            final List<Party> requiredSigners = Arrays.asList(
                    inputLetterOfCredit.getBuyer(),
                    inputLetterOfCredit.getSeller(),
                    inputLetterOfCredit.getIssuingBank(),
                    inputLetterOfCredit.getAdvisingBank()
            );
            Command<PayIssuingBank> txCommand = new Command<>(
                    new PayIssuingBank(),
                    requiredSigners.stream().map(Party::getOwningKey).collect(Collectors.toList())
            );
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputLetterOfCreditRef)
                    .addInputState(inputBillOfLadingRef)
                    .addOutputState(letterOfCredit, LetterOfCreditContract.LOC_CONTRACT_ID)
                    .addOutputState(billOfLadingState, LetterOfCreditContract.LOC_CONTRACT_ID)
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

        StateAndRef<LetterOfCreditState> getLetterOfCreditStateAndRef(String locId) throws FlowException {
            QueryCriteria.VaultQueryCriteria criteria =
                    new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            Vault.Page<LetterOfCreditState> results =
                    getServiceHub().getVaultService().queryBy(LetterOfCreditState.class, criteria);
            List<StateAndRef<LetterOfCreditState>> locList = results.getStates();
            for (StateAndRef<LetterOfCreditState> loc : locList) {
                if (loc.getState().getData().getLocId().equals(locId))
                    return loc;
            }
            throw new FlowException("Unconsumed LetterOfCreditState with ID:" + locId + "not found!");
        }

        StateAndRef<BillOfLadingState> getBillOfLadingStateAndRef(String billOfLadingId) throws FlowException {
            QueryCriteria.VaultQueryCriteria criteria =
                    new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            Vault.Page<BillOfLadingState> results =
                    getServiceHub().getVaultService().queryBy(BillOfLadingState.class, criteria);
            List<StateAndRef<BillOfLadingState>> bills = results.getStates();
            for (StateAndRef<BillOfLadingState> loc : bills) {
                if (loc.getState().getData().getBillOfLadingId().equals(billOfLadingId))
                    return loc;
            }
            throw new FlowException("Unconsumed BillOfLadingState with ID:"
                    + billOfLadingId + "not found! Total bills found: " + bills.size());
        }
    }

    @InitiatedBy(PayIssuingBankFlow.Initiator.class)
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

                        requirements.using(
                                "LetterOfCreditStatus must be ISSUING_BANK_PAID in output LetterOfCreditState.",
                                proposedLetterOfCreditState.getLocStatus().equals("ISSUING_BANK_PAID")
                        );

                        final List<BillOfLadingState> billOfLadingStates =
                                stx.getTx().outputsOfType(BillOfLadingState.class);

                        requirements.using(
                                "There must be exactly 1 BillOfLadingState in output.",
                                billOfLadingStates.size() == 1
                        );

                        if (proposedLetterOfCreditState.getBuyer().equals(ourId)) {
                            requirements.using(
                                    "Issuing Bank should be the owner of BillOfLading.",
                                    billOfLadingStates.get(0).getCurrentOwner().equals(ourId)
                            );
                        }


                        requirements.using(
                                "I (" + getOurIdentity() + ") must be relevant.",
                                proposedLetterOfCreditState.getSeller().equals(ourId) ||
                                        proposedLetterOfCreditState.getAdvisingBank().equals(ourId) ||
                                        proposedLetterOfCreditState.getIssuingBank().equals(ourId)
                        );


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
