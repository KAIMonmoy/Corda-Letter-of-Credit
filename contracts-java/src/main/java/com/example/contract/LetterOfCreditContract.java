package com.example.contract;

import com.example.state.BillOfLadingState;
import com.example.state.LetterOfCreditState;
import com.example.state.PurchaseOrderState;
import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class LetterOfCreditContract implements Contract {
    public static final String LOC_CONTRACT_ID = "com.example.contract.LetterOfCreditContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);

        if (command.getValue() instanceof Commands.CreatePurchaseOrder) {
            requireThat(requirements -> {
                final List<ContractState> inputs = tx.inputsOfType(ContractState.class);

                requirements.using(
                        "There should be no input in CreatePurchaseOrder",
                        inputs.isEmpty()
                );
                final List<PurchaseOrderState> outputs = tx.outputsOfType(PurchaseOrderState.class);

                requirements.using(
                        "Output should have exactly 1 PurchaseOrderState in CreatePurchaseOrder",
                        outputs.size() == 1
                );

                final PurchaseOrderState purchaseOrder = outputs.get(0);
                final Party seller = purchaseOrder.getSeller();
                final Party buyer = purchaseOrder.getBuyer();
                final boolean isAllNumericValuesPositive = purchaseOrder.getProductQuantity() > 0 &&
                        purchaseOrder.getProductPriceInUSD() > 0 &&
                        purchaseOrder.getProductGrossWeightInKG() > 0;

                requirements.using(
                        "The seller and the buyer should not be the same party.",
                        !seller.equals(buyer)
                );

                requirements.using(
                        "All numeric values in the purchase order should be positive.",
                        isAllNumericValuesPositive
                );

                requirements.using(
                        "Seller must be a signer in CreatePurchaseOrder.",
                        command.getSigners().contains(seller.getOwningKey())
                );

                requirements.using(
                        "Buyer must be a signer in CreatePurchaseOrder.",
                        command.getSigners().contains(buyer.getOwningKey())
                );

                return null;
            });
        } else if (command.getValue() instanceof Commands.ApplyForLetterOfCredit) {
            requireThat(requirements -> {
                final List<PurchaseOrderState> inputs = tx.inputsOfType(PurchaseOrderState.class);
                requirements.using(
                        "Input should have exactly 1 PurchaseOrderState in ApplyForLetterOfCredit",
                        inputs.size() == 1
                );

                final List<LetterOfCreditState> outputs = tx.outputsOfType(LetterOfCreditState.class);
                requirements.using(
                        "Output should have exactly 1 LetterOfCreditState in ApplyForLetterOfCredit",
                        outputs.size() == 1
                );

                final PurchaseOrderState inputPurchaseOrder = inputs.get(0);
                final LetterOfCreditState outputLetterOfCreditState = outputs.get(0);

                final boolean isSameSellerAndBuyer =
                        inputPurchaseOrder.getSeller().equals(outputLetterOfCreditState.getSeller()) &&
                        inputPurchaseOrder.getBuyer().equals(outputLetterOfCreditState.getBuyer());
                final boolean isSameProduct =
                        inputPurchaseOrder.getProductQuantity()
                            .equals(outputLetterOfCreditState.getProductQuantity()) &&
                                inputPurchaseOrder.getProductPriceInUSD()
                                    .equals(outputLetterOfCreditState.getProductPriceInUSD()) &&
                                inputPurchaseOrder.getProductGrossWeightInKG()
                                    .equals(outputLetterOfCreditState.getProductGrossWeightInKG()) &&
                                inputPurchaseOrder.getProductName()
                                        .equals(outputLetterOfCreditState.getProductName());
                final boolean isValidLOCValue =
                        outputLetterOfCreditState.getLocValue() > 0 &&
                            outputLetterOfCreditState.getLocValue() >=
                                inputPurchaseOrder.getProductQuantity() * inputPurchaseOrder.getProductPriceInUSD();

                final boolean isAllPartyDistinct = !(
                            outputLetterOfCreditState.getSeller().equals(outputLetterOfCreditState.getIssuingBank()) ||
                            outputLetterOfCreditState.getSeller().equals(outputLetterOfCreditState.getAdvisingBank()) ||
                            outputLetterOfCreditState.getBuyer().equals(outputLetterOfCreditState.getIssuingBank()) ||
                            outputLetterOfCreditState.getBuyer().equals(outputLetterOfCreditState.getAdvisingBank())
                        );

                requirements.using(
                        "Seller & Buyer should be conserved in input & output.",
                        isSameSellerAndBuyer
                );

                requirements.using(
                        "The advisingBank and the issuingBank should not be the same party.",
                        !outputLetterOfCreditState.getAdvisingBank().equals(outputLetterOfCreditState.getIssuingBank())
                );

                requirements.using(
                        "All participants should be distinct parties.",
                        isAllPartyDistinct
                );

                requirements.using(
                        "Product Details should be conserved in input & output.",
                        isSameProduct
                );

                requirements.using(
                        "LOC Value should be positive & sufficient for seller.",
                        isValidLOCValue
                );

                requirements.using(
                        "LetterOfCreditState status should be APPLIED in ApplyForLetterOfCredit.",
                        outputLetterOfCreditState.getLocStatus().equalsIgnoreCase("APPLIED")
                );

                requirements.using(
                        "Buyer must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getBuyer().getOwningKey())
                );

                requirements.using(
                        "Seller must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getSeller().getOwningKey())
                );

                requirements.using(
                        "IssuingBank must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getIssuingBank().getOwningKey())
                );

                requirements.using(
                        "AdvisingBank must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getAdvisingBank().getOwningKey())
                );

                return null;
            });
        } else if (command.getValue() instanceof Commands.ApproveLetterOfCreditApplication) {
            requireThat(requirements -> {
                final List<LetterOfCreditState> inputs = tx.inputsOfType(LetterOfCreditState.class);
                requirements.using(
                        "Input should have exactly 1 LetterOfCreditState in ApproveLetterOfCreditApplication.",
                        inputs.size() == 1
                );

                final List<LetterOfCreditState> outputs = tx.outputsOfType(LetterOfCreditState.class);
                requirements.using(
                        "Output should have exactly 1 LetterOfCreditState in ApproveLetterOfCreditApplication.",
                        outputs.size() == 1
                );

                final LetterOfCreditState inputLetterOfCreditState = inputs.get(0);
                final LetterOfCreditState outputLetterOfCreditState = outputs.get(0);

                requirements.using(
                        "LetterOfCreditState details should be same in input & output.",
                        inputLetterOfCreditState.equalsIgnoreLOCStatus(outputLetterOfCreditState)
                );

                requirements.using(
                        "Input LetterOfCreditState status should be APPLIED in ApproveLetterOfCreditApplication.",
                        inputLetterOfCreditState.getLocStatus().equalsIgnoreCase("APPLIED")
                );

                requirements.using(
                        "Output LetterOfCreditState status should be REJECTED/ISSUED in ApproveLetterOfCreditApplication.",
                        outputLetterOfCreditState.getLocStatus().equalsIgnoreCase("ISSUED") ||
                                outputLetterOfCreditState.getLocStatus().equalsIgnoreCase("REJECTED")
                );

                requirements.using(
                        "Buyer must be a signer in ApproveLetterOfCreditApplication.",
                        command.getSigners().contains(outputLetterOfCreditState.getBuyer().getOwningKey())
                );

                requirements.using(
                        "Seller must be a signer in ApproveLetterOfCreditApplication.",
                        command.getSigners().contains(outputLetterOfCreditState.getSeller().getOwningKey())
                );

                requirements.using(
                        "IssuingBank must be a signer in ApproveLetterOfCreditApplication.",
                        command.getSigners().contains(outputLetterOfCreditState.getIssuingBank().getOwningKey())
                );

                requirements.using(
                        "AdvisingBank must be a signer in ApproveLetterOfCreditApplication.",
                        command.getSigners().contains(outputLetterOfCreditState.getAdvisingBank().getOwningKey())
                );

                return null;
            });
        } else if (command.getValue() instanceof Commands.ShipProducts) {
            requireThat(requirements -> {
                final List<LetterOfCreditState> inputs = tx.inputsOfType(LetterOfCreditState.class);
                requirements.using(
                        "Input should have exactly 1 LetterOfCreditState in ShipProducts.",
                        inputs.size() == 1
                );

                final List<LetterOfCreditState> outputsLetterOfCreditState = tx.outputsOfType(LetterOfCreditState.class);
                requirements.using(
                        "Output should have exactly 1 LetterOfCreditState in ShipProducts.",
                        outputsLetterOfCreditState.size() == 1
                );
                final List<BillOfLadingState> outputsBillOfLadingState = tx.outputsOfType(BillOfLadingState.class);
                requirements.using(
                        "Output should have exactly 1 BillOfLadingState in ShipProducts.",
                        outputsBillOfLadingState.size() == 1
                );

                final LetterOfCreditState inputLetterOfCreditState = inputs.get(0);
                final LetterOfCreditState outputLetterOfCreditState = outputsLetterOfCreditState.get(0);
                final BillOfLadingState outputBillOfLadingState = outputsBillOfLadingState.get(0);

                final boolean isBillOfLadingDetailsSame =
                        inputLetterOfCreditState.getSeller().equals(outputBillOfLadingState.getSeller()) &&
                        inputLetterOfCreditState.getBuyer().equals(outputBillOfLadingState.getBuyer()) &&
                        inputLetterOfCreditState.getAdvisingBank().equals(outputBillOfLadingState.getAdvisingBank()) &&
                        inputLetterOfCreditState.getIssuingBank().equals(outputBillOfLadingState.getIssuingBank()) &&
                        inputLetterOfCreditState.getProductName().equals(outputBillOfLadingState.getProductName()) &&
                        inputLetterOfCreditState.getProductQuantity().equals(outputBillOfLadingState.getProductQuantity()) &&
                        inputLetterOfCreditState.getProductPriceInUSD().equals(outputBillOfLadingState.getProductPriceInUSD()) &&
                        inputLetterOfCreditState.getProductGrossWeightInKG().equals(outputBillOfLadingState.getProductGrossWeightInKG()) &&
                        inputLetterOfCreditState.getLoadingPortAddress().equals(outputBillOfLadingState.getLoadingPortAddress()) &&
                        inputLetterOfCreditState.getLoadingPortCity().equals(outputBillOfLadingState.getLoadingPortCity()) &&
                        inputLetterOfCreditState.getLoadingPortCountry().equals(outputBillOfLadingState.getLoadingPortCountry()) &&
                        inputLetterOfCreditState.getDischargePortAddress().equals(outputBillOfLadingState.getDischargePortAddress()) &&
                        inputLetterOfCreditState.getDischargePortCity().equals(outputBillOfLadingState.getDischargePortCity()) &&
                        inputLetterOfCreditState.getDischargePortCountry().equals(outputBillOfLadingState.getDischargePortCountry());

                requirements.using(
                        "Seller should be the owner in output BillOfLading in ShipProducts.",
                        outputBillOfLadingState.getCurrentOwner().equals(outputBillOfLadingState.getSeller())
                );

                requirements.using(
                        "LetterOfCreditState details should be same in input & output.",
                        inputLetterOfCreditState.equalsIgnoreLOCStatus(outputLetterOfCreditState)
                );

                requirements.using(
                        "BillOfLadingState details should be conserved.",
                        isBillOfLadingDetailsSame
                );

                requirements.using(
                        "Input LetterOfCreditState status should be ISSUED in ShipProducts.",
                        inputLetterOfCreditState.getLocStatus().equalsIgnoreCase("ISSUED")
                );

                requirements.using(
                        "Output LetterOfCreditState status should be SHIPPED in ShipProducts.",
                        outputLetterOfCreditState.getLocStatus().equalsIgnoreCase("SHIPPED")
                );

                requirements.using(
                        "Buyer must be a signer in ShipProducts.",
                        command.getSigners().contains(outputLetterOfCreditState.getBuyer().getOwningKey())
                );

                requirements.using(
                        "Seller must be a signer in ShipProducts.",
                        command.getSigners().contains(outputLetterOfCreditState.getSeller().getOwningKey())
                );

                requirements.using(
                        "IssuingBank must be a signer in ShipProducts.",
                        command.getSigners().contains(outputLetterOfCreditState.getIssuingBank().getOwningKey())
                );

                requirements.using(
                        "AdvisingBank must be a signer in ShipProducts.",
                        command.getSigners().contains(outputLetterOfCreditState.getAdvisingBank().getOwningKey())
                );

                return null;
            });
        } else if (command.getValue() instanceof Commands.PaySeller) {
            requireThat(requirements -> {

                final List<BillOfLadingState> inputsBillOfLadingState = tx.inputsOfType(BillOfLadingState.class);
                requirements.using(
                        "Input should have exactly 1 BillOfLadingState in PaySeller.",
                        inputsBillOfLadingState.size() == 1
                );
                final List<LetterOfCreditState> inputsLetterOfCreditState = tx.inputsOfType(LetterOfCreditState.class);
                requirements.using(
                        "Input should have exactly 1 LetterOfCreditState in PaySeller.",
                        inputsLetterOfCreditState.size() == 1
                );

                final List<LetterOfCreditState> outputs = tx.outputsOfType(LetterOfCreditState.class);
                requirements.using(
                        "Output should have exactly 1 LetterOfCreditState in PaySeller.",
                        outputs.size() == 1
                );
                final List<BillOfLadingState> outputsBillOfLadingState = tx.outputsOfType(BillOfLadingState.class);
                requirements.using(
                        "Output should have exactly 1 BillOfLadingState in PaySeller.",
                        outputsBillOfLadingState.size() == 1
                );

                final LetterOfCreditState inputLetterOfCreditState = inputsLetterOfCreditState.get(0);
                final LetterOfCreditState outputLetterOfCreditState = outputs.get(0);

                requirements.using(
                        "Advising Bank should be the owner in output BillOfLading in PaySeller.",
                        outputsBillOfLadingState.get(0).getCurrentOwner().equals(outputsBillOfLadingState.get(0).getAdvisingBank())
                );

                requirements.using(
                        "LetterOfCreditState details should be same in input & output.",
                        inputLetterOfCreditState.equalsIgnoreLOCStatus(outputLetterOfCreditState)
                );

                requirements.using(
                        "Input LetterOfCreditState status should be SHIPPED in PaySeller.",
                        inputLetterOfCreditState.getLocStatus().equalsIgnoreCase("SHIPPED")
                );

                requirements.using(
                        "Output LetterOfCreditState status should be SELLER_PAID in PaySeller.",
                        outputLetterOfCreditState.getLocStatus().equalsIgnoreCase("SELLER_PAID")
                );

                requirements.using(
                        "Buyer must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getBuyer().getOwningKey())
                );

                requirements.using(
                        "Seller must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getSeller().getOwningKey())
                );

                requirements.using(
                        "IssuingBank must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getIssuingBank().getOwningKey())
                );

                requirements.using(
                        "AdvisingBank must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getAdvisingBank().getOwningKey())
                );

                return null;
            });
        } else if (command.getValue() instanceof Commands.PayAdvisingBank) {
            requireThat(requirements -> {
                final List<BillOfLadingState> inputsBillOfLadingState = tx.inputsOfType(BillOfLadingState.class);
                requirements.using(
                        "Input should have exactly 1 BillOfLadingState in PayAdvisingBank.",
                        inputsBillOfLadingState.size() == 1
                );
                final List<LetterOfCreditState> inputsLetterOfCreditState = tx.inputsOfType(LetterOfCreditState.class);
                requirements.using(
                        "Input should have exactly 1 LetterOfCreditState in PayAdvisingBank.",
                        inputsLetterOfCreditState.size() == 1
                );

                final List<LetterOfCreditState> outputs = tx.outputsOfType(LetterOfCreditState.class);
                requirements.using(
                        "Output should have exactly 1 LetterOfCreditState in PayAdvisingBank.",
                        outputs.size() == 1
                );
                final List<BillOfLadingState> outputsBillOfLadingState = tx.outputsOfType(BillOfLadingState.class);
                requirements.using(
                        "Output should have exactly 1 BillOfLadingState in PayAdvisingBank.",
                        outputsBillOfLadingState.size() == 1
                );

                final LetterOfCreditState inputLetterOfCreditState = inputsLetterOfCreditState.get(0);
                final LetterOfCreditState outputLetterOfCreditState = outputs.get(0);

                requirements.using(
                        "Issuing Bank should be the owner in output BillOfLading in PayAdvisingBank.",
                        outputsBillOfLadingState.get(0).getCurrentOwner().equals(outputsBillOfLadingState.get(0).getIssuingBank())
                );

                requirements.using(
                        "LetterOfCreditState details should be same in input & output.",
                        inputLetterOfCreditState.equalsIgnoreLOCStatus(outputLetterOfCreditState)
                );

                requirements.using(
                        "Input LetterOfCreditState status should be SELLER_PAID in PayAdvisingBank.",
                        inputLetterOfCreditState.getLocStatus().equalsIgnoreCase("SELLER_PAID")
                );

                requirements.using(
                        "Output LetterOfCreditState status should be ADVISING_BANK_PAID in PayAdvisingBank.",
                        outputLetterOfCreditState.getLocStatus().equalsIgnoreCase("ADVISING_BANK_PAID")
                );

                requirements.using(
                        "Buyer must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getBuyer().getOwningKey())
                );

                requirements.using(
                        "Seller must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getSeller().getOwningKey())
                );

                requirements.using(
                        "IssuingBank must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getIssuingBank().getOwningKey())
                );

                requirements.using(
                        "AdvisingBank must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getAdvisingBank().getOwningKey())
                );

                return null;
            });
        } else if (command.getValue() instanceof Commands.PayIssuingBank) {
            requireThat(requirements -> {
                final List<BillOfLadingState> inputsBillOfLadingState = tx.inputsOfType(BillOfLadingState.class);
                requirements.using(
                        "Input should have exactly 1 BillOfLadingState in PayIssuingBank.",
                        inputsBillOfLadingState.size() == 1
                );
                final List<LetterOfCreditState> inputsLetterOfCreditState = tx.inputsOfType(LetterOfCreditState.class);
                requirements.using(
                        "Input should have exactly 1 LetterOfCreditState in PayIssuingBank.",
                        inputsLetterOfCreditState.size() == 1
                );

                final List<LetterOfCreditState> outputs = tx.outputsOfType(LetterOfCreditState.class);
                requirements.using(
                        "Output should have exactly 1 LetterOfCreditState in PayIssuingBank.",
                        outputs.size() == 1
                );
                final List<BillOfLadingState> outputsBillOfLadingState = tx.outputsOfType(BillOfLadingState.class);
                requirements.using(
                        "Output should have exactly 1 BillOfLadingState in PayIssuingBank.",
                        outputsBillOfLadingState.size() == 1
                );

                final LetterOfCreditState inputLetterOfCreditState = inputsLetterOfCreditState.get(0);
                final LetterOfCreditState outputLetterOfCreditState = outputs.get(0);

                requirements.using(
                        "Buyer should be the owner in output BillOfLading in PayIssuingBank.",
                        outputsBillOfLadingState.get(0).getCurrentOwner().equals(outputsBillOfLadingState.get(0).getBuyer())
                );

                requirements.using(
                        "LetterOfCreditState details should be same in input & output.",
                        inputLetterOfCreditState.equalsIgnoreLOCStatus(outputLetterOfCreditState)
                );

                requirements.using(
                        "Input LetterOfCreditState status should be ADVISING_BANK_PAID in PayIssuingBank.",
                        inputLetterOfCreditState.getLocStatus().equalsIgnoreCase("ADVISING_BANK_PAID")
                );

                requirements.using(
                        "Output LetterOfCreditState status should be ISSUING_BANK_PAID in PayIssuingBank.",
                        outputLetterOfCreditState.getLocStatus().equalsIgnoreCase("ISSUING_BANK_PAID")
                );

                requirements.using(
                        "Buyer must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getBuyer().getOwningKey())
                );

                requirements.using(
                        "Seller must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getSeller().getOwningKey())
                );

                requirements.using(
                        "IssuingBank must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getIssuingBank().getOwningKey())
                );

                requirements.using(
                        "AdvisingBank must be a signer in ApplyForLetterOfCredit.",
                        command.getSigners().contains(outputLetterOfCreditState.getAdvisingBank().getOwningKey())
                );

                return null;
            });
        } else {
            throw new IllegalArgumentException("Unknown Command: " + command.getValue());
        }
    }

    public interface Commands extends CommandData {
        class CreatePurchaseOrder implements Commands {}
        class ApplyForLetterOfCredit implements Commands {}
        class ApproveLetterOfCreditApplication implements Commands {}
        class ShipProducts implements Commands {}
        class PaySeller implements Commands {}
        class PayAdvisingBank implements Commands {}
        class PayIssuingBank implements Commands {}
    }
}
