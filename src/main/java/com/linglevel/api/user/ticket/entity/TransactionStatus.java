package com.linglevel.api.user.ticket.entity;

public enum TransactionStatus {
    CONFIRMED("CONFIRMED"),
    RESERVED("RESERVED"), 
    CANCELLED("CANCELLED");
    
    private final String code;
    
    TransactionStatus(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
}