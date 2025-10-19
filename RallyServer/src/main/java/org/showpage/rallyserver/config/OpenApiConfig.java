package org.showpage.rallyserver.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for RallyMaster API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rallyMasterOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RallyMaster API")
                        .description("REST API for managing motorcycle rallies, bonus points, and rider participation")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("RallyMaster Team")
                                .email("support@rallymaster.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT authentication token. Obtain from /api/auth/login or /api/auth/register")));
    }
}
