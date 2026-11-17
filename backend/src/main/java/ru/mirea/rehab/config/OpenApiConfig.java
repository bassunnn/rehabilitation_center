package ru.mirea.rehab.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Конфигурирует OpenAPI / Swagger документацию REST API. */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI rehabOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rehabilitation Center REST API")
                        .version("1.0.0")
                        .description("Единый безопасный REST API информационной системы реабилитационного центра (для Web, JavaFX и Android)"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}

