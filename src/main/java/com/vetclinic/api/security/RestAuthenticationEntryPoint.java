package com.vetclinic.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetclinic.api.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Sem isto, o Spring Security cai no seu entry point padrão (Http403ForbiddenEntryPoint)
 * para rotas protegidas sem autenticação, respondendo 403 em vez de 401 — e nesse formato
 * padrão, diferente do {@link ErrorResponse} usado no resto da API.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ErrorResponse.of(401, "Não autorizado", "Autenticação necessária para acessar este recurso.")
        ));
    }
}
