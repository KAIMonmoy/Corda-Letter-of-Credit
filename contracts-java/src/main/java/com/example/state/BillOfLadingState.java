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
public class BillOfLadingState implements ContractState {
    @NotNull private final String billOfLadingId;
    @NotNull private final Party currentOwner;
    @NotNull private final Party seller;
    @NotNull private final Party buyer;
    @NotNull private final Party advisingBank;
    @NotNull private final Party issuingBank;
    @NotNull private final String carrierCompanyName;
    @NotNull private final String carrierName;
    @NotNull private final String loadingDate;
    @NotNull private final String dischargeDate;
    @NotNull private final String productName;
    @NotNull private final String productDescription;
    @NotNull private final Long productQuantity;
    @NotNull private final Long productPriceInUSD;
    @NotNull private final Long productGrossWeightInKG;
    @NotNull private final String loadingPortAddress;
    @NotNull private final String loadingPortCity;
    @NotNull private final String loadingPortCountry;
    @NotNull private final String dischargePortAddress;
    @NotNull private final String dischargePortCity;
    @NotNull private final String dischargePortCountry;

    public BillOfLadingState(@NotNull String billOfLadingId,
                             @NotNull Party currentOwner,
                             @NotNull Party seller,
                             @NotNull Party buyer,
                             @NotNull Party advisingBank,
                             @NotNull Party issuingBank,
                             @NotNull String carrierCompanyName,
                             @NotNull String carrierName,
                             @NotNull String loadingDate,
                             @NotNull String dischargeDate,
                             @NotNull String productName,
                             @NotNull String productDescription,
                             @NotNull Long productQuantity,
                             @NotNull Long productPriceInUSD,
                             @NotNull Long productGrossWeightInKG,
                             @NotNull String loadingPortAddress,
                             @NotNull String loadingPortCity,
                             @NotNull String loadingPortCountry,
                             @NotNull String dischargePortAddress,
                             @NotNull String dischargePortCity,
                             @NotNull String dischargePortCountry) {
        this.billOfLadingId = billOfLadingId;
        this.currentOwner = currentOwner;
        this.seller = seller;
        this.buyer = buyer;
        this.advisingBank = advisingBank;
        this.issuingBank = issuingBank;
        this.carrierCompanyName = carrierCompanyName;
        this.carrierName = carrierName;
        this.loadingDate = loadingDate;
        this.dischargeDate = dischargeDate;
        this.productName = productName;
        this.productDescription = productDescription;
        this.productQuantity = productQuantity;
        this.productPriceInUSD = productPriceInUSD;
        this.productGrossWeightInKG = productGrossWeightInKG;
        this.loadingPortAddress = loadingPortAddress;
        this.loadingPortCity = loadingPortCity;
        this.loadingPortCountry = loadingPortCountry;
        this.dischargePortAddress = dischargePortAddress;
        this.dischargePortCity = dischargePortCity;
        this.dischargePortCountry = dischargePortCountry;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(seller, buyer, advisingBank, issuingBank);
    }

    @NotNull
    public Party getCurrentOwner() {
        return currentOwner;
    }

    @NotNull
    public Party getAdvisingBank() {
        return advisingBank;
    }

    @NotNull
    public Party getIssuingBank() {
        return issuingBank;
    }

    @NotNull
    public String getBillOfLadingId() {
        return billOfLadingId;
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
    public String getCarrierCompanyName() {
        return carrierCompanyName;
    }

    @NotNull
    public String getCarrierName() {
        return carrierName;
    }

    @NotNull
    public String getLoadingDate() {
        return loadingDate;
    }

    @NotNull
    public String getDischargeDate() {
        return dischargeDate;
    }

    @NotNull
    public String getProductName() {
        return productName;
    }

    @NotNull
    public String getProductDescription() {
        return productDescription;
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
    public Long getProductGrossWeightInKG() {
        return productGrossWeightInKG;
    }

    @NotNull
    public String getLoadingPortAddress() {
        return loadingPortAddress;
    }

    @NotNull
    public String getLoadingPortCity() {
        return loadingPortCity;
    }

    @NotNull
    public String getLoadingPortCountry() {
        return loadingPortCountry;
    }

    @NotNull
    public String getDischargePortAddress() {
        return dischargePortAddress;
    }

    @NotNull
    public String getDischargePortCity() {
        return dischargePortCity;
    }

    @NotNull
    public String getDischargePortCountry() {
        return dischargePortCountry;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                billOfLadingId,
                currentOwner,
                seller,
                buyer,
                advisingBank,
                issuingBank,
                carrierCompanyName,
                carrierName,
                loadingDate,
                dischargeDate,
                productName,
                productDescription,
                productQuantity,
                productPriceInUSD,
                productGrossWeightInKG,
                loadingPortAddress,
                loadingPortCity,
                loadingPortCountry,
                dischargePortAddress,
                dischargePortCity,
                dischargePortCountry
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final BillOfLadingState that = (BillOfLadingState) obj;

        return billOfLadingId.equals(that.billOfLadingId) &&
        currentOwner.equals(that.currentOwner) &&
        seller.equals(that.seller) &&
        buyer.equals(that.buyer) &&
        advisingBank.equals(that.advisingBank) &&
        issuingBank.equals(that.issuingBank) &&
        carrierCompanyName.equals(that.carrierCompanyName) &&
        carrierName.equals(that.carrierName) &&
        loadingDate.equals(that.loadingDate) &&
        dischargeDate.equals(that.dischargeDate) &&
        productName.equals(that.productName) &&
        productDescription.equals(that.productDescription) &&
        productQuantity.equals(that.productQuantity) &&
        productPriceInUSD.equals(that.productPriceInUSD) &&
        productGrossWeightInKG.equals(that.productGrossWeightInKG) &&
        loadingPortAddress.equals(that.loadingPortAddress) &&
        loadingPortCity.equals(that.loadingPortCity) &&
        loadingPortCountry.equals(that.loadingPortCountry) &&
        dischargePortAddress.equals(that.dischargePortAddress) &&
        dischargePortCity.equals(that.dischargePortCity) &&
        dischargePortCountry.equals(that.dischargePortCountry);
    }

    @Override
    public String toString() {
        return "BillOfLadingState{" +
                " billOfLadingId: " + billOfLadingId +
                " currentOwner: " + currentOwner +
                " seller: " + seller +
                " buyer: " + buyer +
                " advisingBank: " + advisingBank +
                " issuingBank: " + issuingBank +
                " carrierCompanyName: " + carrierCompanyName +
                " carrierName: " + carrierName +
                " loadingDate: " + loadingDate +
                " dischargeDate: " + dischargeDate +
                " productName: " + productName +
                " productDescription: " + productDescription +
                " productQuantity: " + productQuantity +
                " productPriceInUSD: " + productPriceInUSD +
                " productGrossWeightInKG: " + productGrossWeightInKG +
                " loadingPortAddress: " + loadingPortAddress +
                " loadingPortCity: " + loadingPortCity +
                " loadingPortCountry: " + loadingPortCountry +
                " dischargePortAddress: " + dischargePortAddress +
                " dischargePortCity: " + dischargePortCity +
                " dischargePortCountry: " + dischargePortCountry +
                " }";
    }
}
