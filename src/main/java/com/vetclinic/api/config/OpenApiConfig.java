package com.vetclinic.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configura os metadados e o esquema de segurança (Bearer JWT) exibidos
 * na UI do Swagger, disponível em /docs.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Vet Clinic API",
                version = "v1",
                description = "API REST para gestão de uma clínica veterinária: clientes (tutores), pets, "
                        + "agendamento de consultas, prontuários médicos e catálogo de serviços.",
                contact = @Contact(name = "Pedro Linard")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
