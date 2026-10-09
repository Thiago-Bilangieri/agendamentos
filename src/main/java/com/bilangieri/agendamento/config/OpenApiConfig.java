package com.bilangieri.agendamento.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    // Nomes usados em @Tag nos controllers; a ordem aqui é a ordem no Swagger UI
    public static final String TAG_AUTH = "Autenticação";
    public static final String TAG_SERVICES = "Serviços";
    public static final String TAG_CATEGORIES = "Categorias";
    public static final String TAG_PROFESSIONALS = "Prestadores";
    public static final String TAG_APPOINTMENTS = "Agendamentos";
    public static final String TAG_CUSTOMERS = "Clientes";
    public static final String TAG_ADMIN = "Administração";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Agendamento API")
                        .version("v1")
                        .description("""
                                API de agendamento de serviços entre clientes e prestadores.

                                **Como autenticar:** faça `POST /api/auth/login`, copie o `token` da resposta, \
                                clique em **Authorize** e cole-o (sem o prefixo `Bearer`).

                                **Perfis:** `ADMIN`, `PROFESSIONAL` e `CUSTOMER`. Cada endpoint indica quem o pode usar; \
                                um perfil sem permissão recebe `403`.

                                **Listagens paginadas:** `?page=0&size=20&sort=campo,asc` (máximo de 100 por página). \
                                A resposta tem o formato `{ "content": [...], "page": {...} }`.

                                **Erros:** formato Problem Details (RFC 9457, `application/problem+json`): \
                                `{ "title", "status", "detail", "instance", "timestamp" }`; erros de validação trazem \
                                `errors` com o erro de cada campo.""")
                        )
                .tags(List.of(
                        new Tag().name(TAG_AUTH).description("Registo e login (públicos)"),
                        new Tag().name(TAG_SERVICES).description("Catálogo de serviços, filtros e horários livres"),
                        new Tag().name(TAG_CATEGORIES).description("Categorias de serviços"),
                        new Tag().name(TAG_PROFESSIONALS).description("Horário de trabalho dos prestadores"),
                        new Tag().name(TAG_APPOINTMENTS).description("Marcações e ciclo de vida do agendamento"),
                        new Tag().name(TAG_CUSTOMERS).description("Clientes"),
                        new Tag().name(TAG_ADMIN).description("Aprovação de prestadores")))
                // Aplica o JWT a todos os endpoints; o botão "Authorize" do Swagger UI envia o header
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    // Respostas comuns a todos os endpoints protegidos, para não as repetir em cada controller.
    // Os endpoints públicos declaram @SecurityRequirements() vazio e ficam de fora.
    @Bean
    public OpenApiCustomizer securedEndpointResponses() {
        return openApi -> openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
            boolean isPublic = operation.getSecurity() != null && operation.getSecurity().isEmpty();
            if (isPublic) {
                return;
            }
            operation.getResponses().putIfAbsent("401", new ApiResponse().description("Token em falta, inválido ou expirado"));
            operation.getResponses().putIfAbsent("403", new ApiResponse().description("O perfil autenticado não tem permissão"));
        }));
    }
}
