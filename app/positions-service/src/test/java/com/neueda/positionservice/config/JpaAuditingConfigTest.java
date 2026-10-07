package com.neueda.positionservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JpaAuditingConfigTest {

    private JpaAuditingConfig config;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;

    @BeforeEach
    public void setUp() {
        config = new JpaAuditingConfig();
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("auditorAware returns authenticated user name")
    public void testAuditorAwareWithAuthenticatedUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        
        AuditorAware<String> auditorAware = config.auditorAware();
        Optional<String> auditor = auditorAware.getCurrentAuditor();
        
        assertTrue(auditor.isPresent());
        assertEquals("testuser", auditor.get());
    }

    @Test
    @DisplayName("auditorAware returns SYSTEM when no authentication")
    public void testAuditorAwareNoAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);
        
        AuditorAware<String> auditorAware = config.auditorAware();
        Optional<String> auditor = auditorAware.getCurrentAuditor();
        
        assertTrue(auditor.isPresent());
        assertEquals("SYSTEM", auditor.get());
    }

    @Test
    @DisplayName("auditorAware returns SYSTEM for unauthenticated user")
    public void testAuditorAwareUnauthenticatedUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);
        
        AuditorAware<String> auditorAware = config.auditorAware();
        Optional<String> auditor = auditorAware.getCurrentAuditor();
        
        assertTrue(auditor.isPresent());
        assertEquals("SYSTEM", auditor.get());
    }

    @Test
    @DisplayName("auditorAware returns SYSTEM for AnonymousAuthenticationToken")
    public void testAuditorAwareAnonymousUser() {
        AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken("key", "anonymousUser", 
            java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        when(securityContext.getAuthentication()).thenReturn(anonymousToken);
        
        AuditorAware<String> auditorAware = config.auditorAware();
        Optional<String> auditor = auditorAware.getCurrentAuditor();
        
        assertTrue(auditor.isPresent());
        assertEquals("SYSTEM", auditor.get());
    }

    @Test
    @DisplayName("auditorAware bean is not null")
    public void testAuditorAwareBeanNotNull() {
        AuditorAware<String> auditorAware = config.auditorAware();
        assertNotNull(auditorAware);
    }
}
