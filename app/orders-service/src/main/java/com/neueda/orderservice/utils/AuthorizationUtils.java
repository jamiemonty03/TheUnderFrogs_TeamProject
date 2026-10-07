package com.neueda.orderservice.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import com.neueda.orderservice.exceptions.AccountAuthorizationException;
import java.util.Collection;
import java.util.Collections;

public class AuthorizationUtils {

    private AuthorizationUtils() {}

    public static String getAccountIdFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("accountId");
        }
        return null;
    }

    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            return authorities.stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        }
        return false;
    }

    public static boolean isService() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            return authorities.stream()
                    .anyMatch(a -> "ROLE_SERVICE".equals(a.getAuthority()));
        }
        return false;
    }

    public static void verifyAccountAccess(String requestedAccountId) throws AccountAuthorizationException {
        if (isAdmin() || isService()) {
            return;
        }

        String userAccountId = getAccountIdFromToken();
        if (userAccountId == null || !userAccountId.equals(requestedAccountId)) {
            throw new AccountAuthorizationException(
                    "User is not authorized to access account: " + requestedAccountId);
        }
    }
}
