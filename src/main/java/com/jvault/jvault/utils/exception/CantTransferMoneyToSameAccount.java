package com.jvault.jvault.utils.exception;

public class CantTransferMoneyToSameAccount extends RuntimeException {
    public CantTransferMoneyToSameAccount(String message) {
        super(message);
    }
}
