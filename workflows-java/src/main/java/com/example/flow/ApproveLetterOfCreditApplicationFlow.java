package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.LetterOfCreditContract;
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

import static com.example.contract.LetterOfCreditContract.Commands.ApproveLetterOfCreditApplication;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public interface ApproveLetterOfCreditApplicationFlow {
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
        @NotNull private final String locStatus;

        public Initiator(@NotNull String locId, @NotNull String locStatus) {
            this.locId = locId;
            this.locStatus = locStatus;
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
            if (!inputLetterOfCreditRef.getState().getData().getIssuingBank().equals(getOurIdentity()))
                throw new FlowException("I (" + getOurIdentity() + ") must be the issuing bank in the referenced proposed LOC.");
            if (!inputLetterOfCredit.getLocStatus().equals("APPLIED"))
                throw new FlowException("Invalid locStatus:" + inputLetterOfCredit.getLocStatus() + " found. Required locStatus: APPLIED");
            if (!(locStatus.equals("ISSUED") || locStatus.equals("REJECTED")))
                throw new FlowException("Proposed LOC Status must be ISSUED/REJECTED. Found " + locStatus + ".");
            final LetterOfCreditState letterOfCredit = LetterOfCreditState.locWithUpdatedStatus(
                    inputLetterOfCredit,
                    locStatus
            );
            final List<Party> requiredSigners = Arrays.asList(
                    inputLetterOfCredit.getBuyer(),
                    inputLetterOfCredit.getSeller(),
                    inputLetterOfCredit.getIssuingBank(),
                    inputLetterOfCredit.getAdvisingBank()
            );
            Command<ApproveLetterOfCreditApplication> txCommand = new Command<>(
                    new ApproveLetterOfCreditApplication(),
                    requiredSigners.stream().map(Party::getOwningKey).collect(Collectors.toList())
            );
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputLetterOfCreditRef)
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

        StateAndRef<LetterOfCreditState> getLetterOfCreditStateAndRef(String locId) throws FlowException {
            QueryCriteria.VaultQueryCriteria criteria =
                    new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            Vault.Page<LetterOfCreditState> results =
                    getServiceHub().getVaultService().queryBy(LetterOfCreditState.class, criteria);
            List<StateAndRef<LetterOfCreditState>> purchaseOrders = results.getStates();
            for (StateAndRef<LetterOfCreditState> loc : purchaseOrders) {
                if (loc.getState().getData().getLocId().equals(locId))
                    return loc;
            }
            throw new FlowException("Unconsumed LetterOfCreditState with ID:" + locId + "not found!");
        }
    }

    @InitiatedBy(ApproveLetterOfCreditApplicationFlow.Initiator.class)
    class Responder extends FlowLogic<SignedTransaction> {

        private final FlowSession issuingBankSession;

        public Responder(FlowSession issuingBankSession) { this.issuingBankSession = issuingBankSession; }

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
                                "LetterOfCreditStatus must be APPROVED/REJECTED in output LetterOfCreditState.",
                                proposedLetterOfCreditState.getLocStatus().equals("ISSUED") ||
                                        proposedLetterOfCreditState.getLocStatus().equals("REJECTED")
                        );


                        requirements.using(
                                "I (" + getOurIdentity() + ") must be relevant.",
                                proposedLetterOfCreditState.getSeller().equals(ourId) ||
                                        proposedLetterOfCreditState.getAdvisingBank().equals(ourId) ||
                                        proposedLetterOfCreditState.getBuyer().equals(ourId)
                        );


                        return null;
                    });
                }
            }
            final SignTxFlow signTxFlow = new SignTxFlow(issuingBankSession,
                    SignTransactionFlow.Companion.tracker(),
                    getOurIdentity());
            final SecureHash txId = subFlow(signTxFlow).getId();

            return subFlow(new ReceiveFinalityFlow(issuingBankSession, txId));
        }
    }
}
