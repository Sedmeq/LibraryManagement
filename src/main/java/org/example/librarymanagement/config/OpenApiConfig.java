package org.example.librarymanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI libraryManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Library Management API")
                        .description("Book, Author, Member üzərində layered architecture ilə CRUD REST API")
                        .version("v1.0.0")
                        .contact(new Contact().name("Library Management Team")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("JWT token daxil edin (\"Bearer \" prefiksini əlavə etməyə ehtiyac yoxdur — Swagger özü əlavə edir)")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
    }
}

