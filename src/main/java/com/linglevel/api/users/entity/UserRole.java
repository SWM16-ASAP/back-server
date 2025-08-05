package com.linglevel.api.users.entity;

public enum UserRole {
    ADMIN,
    USER;

    // for spring security GrantedAuthority
    public String getSecurityRole() {
        return "ROLE_" + this.name();
    }
}