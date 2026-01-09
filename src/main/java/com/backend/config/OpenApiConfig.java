package com.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Eros Attendance API",
        version = "1.0",
        description = "REST API for Eros Attendance System with PASETO Authentication",
        contact = @Contact(
            name = "Eros Team",
            email = "support@eros.com"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local Development Server"),
        @Server(url = "http://129.226.159.225:8080", description = "VPS Production Server"),
        @Server(url = "/", description = "Current Server (Dynamic)")
    }
)
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "PASETO",
    scheme = "bearer",
    description = "Enter your PASETO access token"
)
public class OpenApiConfig {
}
