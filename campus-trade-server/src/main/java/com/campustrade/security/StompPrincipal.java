package com.campustrade.security;

import java.security.Principal;

public class StompPrincipal implements Principal {

    private final Long userId;
    private final String username;

    public StompPrincipal(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    public String getUsername() {
        return username;
    }
}