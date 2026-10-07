package com.neueda.positionservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OpenApiConfigTest {

    private OpenApiConfig config;

    @BeforeEach
    public void setUp() {
        config = new OpenApiConfig();
    }

    @Test
    @DisplayName("OpenApiConfig creates OpenAPI bean with correct title")
    public void testCustomOpenAPITitle() {
        OpenAPI openAPI = config.customOpenAPI();
        
        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("Positions Service API", openAPI.getInfo().getTitle());
    }

    @Test
    @DisplayName("OpenApiConfig creates OpenAPI bean with correct version")
    public void testCustomOpenAPIVersion() {
        OpenAPI openAPI = config.customOpenAPI();
        
        assertNotNull(openAPI);
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
    }

    @Test
    @DisplayName("OpenApiConfig creates OpenAPI bean with correct description")
    public void testCustomOpenAPIDescription() {
        OpenAPI openAPI = config.customOpenAPI();
        
        assertNotNull(openAPI);
        assertTrue(openAPI.getInfo().getDescription().contains("position management"));
    }

    @Test
    @DisplayName("OpenApiConfig includes JWT security scheme")
    public void testCustomOpenAPISecurityScheme() {
        OpenAPI openAPI = config.customOpenAPI();
        
        assertNotNull(openAPI);
        assertNotNull(openAPI.getComponents());
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("bearer-jwt"));
    }

    @Test
    @DisplayName("OpenApiConfig security scheme is HTTP Bearer type")
    public void testCustomOpenAPISecuritySchemeType() {
        OpenAPI openAPI = config.customOpenAPI();
        
        assertNotNull(openAPI);
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("bearer-jwt");
        assertNotNull(scheme);
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
    }

    @Test
    @DisplayName("OpenApiConfig includes security requirement")
    public void testCustomOpenAPISecurityRequirement() {
        OpenAPI openAPI = config.customOpenAPI();
        
        assertNotNull(openAPI);
        assertNotNull(openAPI.getSecurity());
        assertFalse(openAPI.getSecurity().isEmpty());
    }

    @Test
    @DisplayName("OpenApiConfig bean is not null")
    public void testCustomOpenAPIBeanNotNull() {
        OpenAPI openAPI = config.customOpenAPI();
        assertNotNull(openAPI);
    }
}
