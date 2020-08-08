package com.example.state;

import com.example.contract.LetterOfCreditContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@BelongsToContract(LetterOfCreditContract.class)
public class PurchaseOrderState implements ContractState {
    @NotNull private final String purchaseOrderId;
    @NotNull private final Party seller;
    @NotNull private final Party buyer;
    @NotNull private final String purchaseOrderIssueDate;
    @NotNull private final String productName;
    @NotNull private final Long productQuantity;
    @NotNull private final Long productPriceInUSD;
    @NotNull private final Long productPriceInKG;

    public PurchaseOrderState(@NotNull String purchaseOrderId,
                              @NotNull Party seller,
                              @NotNull Party buyer,
                              @NotNull String purchaseOrderIssueDate,
                              @NotNull String productName,
                              @NotNull Long productQuantity,
                              @NotNull Long productPriceInUSD,
                              @NotNull Long productPriceInKG) {
        this.purchaseOrderId = purchaseOrderId;
        this.seller = seller;
        this.buyer = buyer;
        this.purchaseOrderIssueDate = purchaseOrderIssueDate;
        this.productName = productName;
        this.productQuantity = productQuantity;
        this.productPriceInUSD = productPriceInUSD;
        this.productPriceInKG = productPriceInKG;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(seller, buyer);
    }

    @NotNull
    public String getPurchaseOrderId() {
        return purchaseOrderId;
    }

    @NotNull
    public Party getSeller() {
        return seller;
    }

    @NotNull
    public Party getBuyer() {
        return buyer;
    }

    @NotNull
    public String getPurchaseOrderIssueDate() {
        return purchaseOrderIssueDate;
    }

    @NotNull
    public String getProductName() {
        return productName;
    }

    @NotNull
    public Long getProductQuantity() {
        return productQuantity;
    }

    @NotNull
    public Long getProductPriceInUSD() {
        return productPriceInUSD;
    }

    @NotNull
    public Long getProductPriceInKG() {
        return productPriceInKG;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final PurchaseOrderState that = (PurchaseOrderState) obj;
        return this.purchaseOrderId.equals(that.purchaseOrderId) &&
        this.seller.equals(that.seller) &&
        this.buyer.equals(that.buyer) &&
        this.purchaseOrderIssueDate.equals(that.purchaseOrderIssueDate) &&
        this.productName.equals(that.productName) &&
        this.productQuantity.equals(that.productQuantity) &&
        this.productPriceInUSD.equals(that.productPriceInUSD) &&
        this.productPriceInKG.equals(that.productPriceInKG);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                purchaseOrderId,
                seller,
                buyer,
                purchaseOrderIssueDate,
                productName,
                productQuantity,
                productPriceInUSD,
                productPriceInKG
            );
    }

    @Override
    public String toString() {
        return "PurchaseOrderState{" +
                " purchaseOrderId: " + purchaseOrderId +
                " seller: " + seller +
                " buyer: " + buyer +
                " purchaseOrderIssueDate: " + purchaseOrderIssueDate +
                " productName: " + productName +
                " productQuantity: " + productQuantity +
                " productPriceInUSD: " + productPriceInUSD +
                " productPriceInKG: " + productPriceInKG +
                " }";
    }
}
