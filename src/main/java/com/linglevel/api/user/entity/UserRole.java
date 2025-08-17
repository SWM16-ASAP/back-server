package com.linglevel.api.user.entity;

public enum UserRole {
    ADMIN,
    USER;

    // for spring security GrantedAuthority
    public String getSecurityRole() {
        return "ROLE_" + this.name();
    }
}