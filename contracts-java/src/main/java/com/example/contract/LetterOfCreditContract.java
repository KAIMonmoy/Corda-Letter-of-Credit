package com.example.contract;

import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class LetterOfCreditContract implements Contract {
    public static final String ID = "com.example.contract.LetterOfCreditContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

    }
}
