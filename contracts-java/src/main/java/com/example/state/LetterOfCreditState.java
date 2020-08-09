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
public class LetterOfCreditState implements ContractState {
    @NotNull private final String locId;
    @NotNull private final String locType;
    @NotNull private final String locExpiryDate;
    @NotNull private final Party seller;
    @NotNull private final Party buyer;
    @NotNull private final Party advisingBank;
    @NotNull private final Party issuingBank;
    @NotNull private final Long locValue;
    @NotNull private final String loadingPortAddress;
    @NotNull private final String loadingPortCity;
    @NotNull private final String loadingPortCountry;
    @NotNull private final String dischargePortAddress;
    @NotNull private final String dischargePortCity;
    @NotNull private final String dischargePortCountry;
    @NotNull private final String productName;
    @NotNull private final Long productQuantity;
    @NotNull private final Long productPriceInUSD;
    @NotNull private final Long productPriceInKG;
    /**
     // locStatus LifeCycle
     // APPLIED -> REJECTED
     // APPLIED -> ISSUED -> SHIPPED -> SELLER_PAID -> ADVISING_BANK_PAID -> ISSUING_BANK_PAID
     */
    @NotNull private final String locStatus;

    public LetterOfCreditState(@NotNull String locId,
                               @NotNull String locType,
                               @NotNull String locExpiryDate,
                               @NotNull Party seller,
                               @NotNull Party buyer,
                               @NotNull Party advisingBank,
                               @NotNull Party issuingBank,
                               @NotNull Long locValue,
                               @NotNull String loadingPortAddress,
                               @NotNull String loadingPortCity,
                               @NotNull String loadingPortCountry,
                               @NotNull String dischargePortAddress,
                               @NotNull String dischargePortCity,
                               @NotNull String dischargePortCountry,
                               @NotNull String productName,
                               @NotNull Long productQuantity,
                               @NotNull Long productPriceInUSD,
                               @NotNull Long productPriceInKG,
                               @NotNull String locStatus
    ) {
        this.locId = locId;
        this.locType = locType;
        this.locExpiryDate = locExpiryDate;
        this.seller = seller;
        this.buyer = buyer;
        this.advisingBank = advisingBank;
        this.issuingBank = issuingBank;
        this.locValue = locValue;
        this.loadingPortAddress = loadingPortAddress;
        this.loadingPortCity = loadingPortCity;
        this.loadingPortCountry = loadingPortCountry;
        this.dischargePortAddress = dischargePortAddress;
        this.dischargePortCity = dischargePortCity;
        this.dischargePortCountry = dischargePortCountry;
        this.productName = productName;
        this.productQuantity = productQuantity;
        this.productPriceInUSD = productPriceInUSD;
        this.productPriceInKG = productPriceInKG;
        this.locStatus = locStatus;
    }

    public LetterOfCreditState(@NotNull String locId,
                               @NotNull String locType,
                               @NotNull String locExpiryDate,
                               @NotNull Party seller,
                               @NotNull Party buyer,
                               @NotNull Party advisingBank,
                               @NotNull Party issuingBank,
                               @NotNull Long locValue,
                               @NotNull String loadingPortAddress,
                               @NotNull String loadingPortCity,
                               @NotNull String loadingPortCountry,
                               @NotNull String dischargePortAddress,
                               @NotNull String dischargePortCity,
                               @NotNull String dischargePortCountry,
                               @NotNull String productName,
                               @NotNull Long productQuantity,
                               @NotNull Long productPriceInUSD,
                               @NotNull Long productPriceInKG) {
        this(
                locId,
                locType,
                locExpiryDate,
                seller,
                buyer,
                advisingBank,
                issuingBank,
                locValue,
                loadingPortAddress,
                loadingPortCity,
                loadingPortCountry,
                dischargePortAddress,
                dischargePortCity,
                dischargePortCountry,
                productName,
                productQuantity,
                productPriceInUSD,
                productPriceInKG,
                "APPLIED"
        );
    }

    public static LetterOfCreditState locWithUpdatedStatus(LetterOfCreditState currentLOC, String newStatus) {
        return new LetterOfCreditState(
                currentLOC.locId,
                currentLOC.locType,
                currentLOC.locExpiryDate,
                currentLOC.seller,
                currentLOC.buyer,
                currentLOC.advisingBank,
                currentLOC.issuingBank,
                currentLOC.locValue,
                currentLOC.loadingPortAddress,
                currentLOC.loadingPortCity,
                currentLOC.loadingPortCountry,
                currentLOC.dischargePortAddress,
                currentLOC.dischargePortCity,
                currentLOC.dischargePortCountry,
                currentLOC.productName,
                currentLOC.productQuantity,
                currentLOC.productPriceInUSD,
                currentLOC.productPriceInKG,
                newStatus
        );
    }


    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(seller, buyer, issuingBank, advisingBank);
    }

    @NotNull
    public String getLocStatus() {
        return locStatus;
    }

    @NotNull
    public String getLocId() {
        return locId;
    }

    @NotNull
    public String getLocType() {
        return locType;
    }

    @NotNull
    public String getLocExpiryDate() {
        return locExpiryDate;
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
    public Party getAdvisingBank() {
        return advisingBank;
    }

    @NotNull
    public Party getIssuingBank() {
        return issuingBank;
    }

    @NotNull
    public Long getLocValue() {
        return locValue;
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
    public int hashCode() {
        return Objects.hash(
                locId,
                locType,
                locExpiryDate,
                seller,
                buyer,
                advisingBank,
                issuingBank,
                locValue,
                loadingPortAddress,
                loadingPortCity,
                loadingPortCountry,
                dischargePortAddress,
                dischargePortCity,
                dischargePortCountry,
                productName,
                productQuantity,
                productPriceInUSD,
                productPriceInKG,
                locStatus
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final LetterOfCreditState that = (LetterOfCreditState) obj;

        return locId.equals(that.locId) &&
        locType.equals(that.locType) &&
        locExpiryDate.equals(that.locExpiryDate) &&
        seller.equals(that.seller) &&
        buyer.equals(that.buyer) &&
        advisingBank.equals(that.advisingBank) &&
        issuingBank.equals(that.issuingBank) &&
        locValue.equals(that.locValue) &&
        loadingPortAddress.equals(that.loadingPortAddress) &&
        loadingPortCity.equals(that.loadingPortCity) &&
        loadingPortCountry.equals(that.loadingPortCountry) &&
        dischargePortAddress.equals(that.dischargePortAddress) &&
        dischargePortCity.equals(that.dischargePortCity) &&
        dischargePortCountry.equals(that.dischargePortCountry) &&
        productName.equals(that.productName) &&
        productQuantity.equals(that.productQuantity) &&
        productPriceInUSD.equals(that.productPriceInUSD) &&
        productPriceInKG.equals(that.productPriceInKG) &&
        locStatus.equals(that.locStatus);
    }

    @Override
    public String toString() {
        return "LetterOfCreditState{" +
                " locId: " + locId +
                " locType: " + locType +
                " locExpiryDate: " + locExpiryDate +
                " seller: " + seller +
                " buyer: " + buyer +
                " advisingBank: " + advisingBank +
                " issuingBank: " + issuingBank +
                " locValue: " + locValue +
                " loadingPortAddress: " + loadingPortAddress +
                " loadingPortCity: " + loadingPortCity +
                " loadingPortCountry: " + loadingPortCountry +
                " dischargePortAddress: " + dischargePortAddress +
                " dischargePortCity: " + dischargePortCity +
                " dischargePortCountry: " + dischargePortCountry +
                " productName: " + productName +
                " productQuantity: " + productQuantity +
                " productPriceInUSD: " + productPriceInUSD +
                " productPriceInKG: " + productPriceInKG +
                " locStatus: " + locStatus +
                " }";
    }
}
