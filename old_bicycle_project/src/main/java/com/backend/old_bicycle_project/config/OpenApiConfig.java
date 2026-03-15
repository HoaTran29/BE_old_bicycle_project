package com.backend.old_bicycle_project.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Old Bicycle Marketplace API",
                version = "v1",
                description = "Tài liệu API cho backend marketplace xe đạp cũ."
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .servers(List.of(new Server()
                        .url("/")
                        .description("Current origin")))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new io.swagger.v3.oas.models.security.SecurityScheme()
                                .type(Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(In.HEADER)
                ))
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Old Bicycle Marketplace API")
                        .version("v1")
                        .description("Swagger UI để test REST API của dự án.")
                        .contact(new Contact().name("Old Bicycle Backend")));
    }
}
