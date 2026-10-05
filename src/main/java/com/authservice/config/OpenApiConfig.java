package com.authservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Auth Microservice API")
                .version("1.0.0")
                .description("Authentication and authorization API for user registration, login, token refresh, password recovery, and account management.")
                .contact(new Contact()
                    .name("Auth Service Team")
                    .email("support@authservice.local"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")));
    }
}
