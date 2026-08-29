package com.vetclinic.api.config;

import com.vetclinic.api.security.CustomUserDetailsService;
import com.vetclinic.api.security.JwtAuthenticationFilter;
import com.vetclinic.api.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize nos controllers/services
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final Environment environment;

    /**
     * CSP restritiva para as páginas servidas em produção (landing + SPA):
     * a SPA não tem nenhum <script> inline, então <code>script-src 'self'</code>
     * bloqueia injeção de script — a principal mitigação de XSS aqui. Estilos
     * inline ainda são usados, daí <code>'unsafe-inline'</code> só em style-src.
     * Em dev a CSP é afrouxada para não quebrar a UI do Swagger.
     */
    private static final String CSP_PROD =
            "default-src 'self'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'; "
                    + "object-src 'none'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                    + "script-src 'self'; connect-src 'self'";

    private static final String CSP_DEV =
            "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                    + "script-src 'self' 'unsafe-inline'; connect-src 'self'";

    // Lista separada por vírgula. Em dev/test, "*" libera qualquer origem por
    // conveniência; em produção, deve apontar para o(s) domínio(s) reais do
    // frontend (ver CORS_ALLOWED_ORIGINS no application-prod.yml / docker-compose.yml).
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOriginsCsv;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/",
            "/index.html",
            "/app/**",
            "/api/auth/login",
            "/docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health",
            // Sem isso, um 403 lançado no nível de filtro (ex: a regra do /actuator/**
            // abaixo) gera um forward interno pro /error que — sem estar liberado —
            // é reavaliado do zero pela cadeia de segurança; nesse segundo passo o
            // contexto já não carrega mais a autenticação original, o que faz o
            // pedido parecer anônimo e vira um 401 confuso em vez do 403 esperado.
            "/error"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean devProfile = environment.matchesProfiles("dev");

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll();
                    // O console do H2 só é liberado no perfil dev — assim a regra não
                    // sobrevive ao servlet, que também só existe em dev.
                    if (devProfile) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    // /actuator/health fica público (usado por health checks de infra); o
                    // resto do Actuator (métricas, env, etc.) expõe detalhes internos, então
                    // fica restrito a ADMIN em vez de "qualquer autenticado".
                    auth.requestMatchers("/actuator/**").hasRole("ADMIN")
                            .anyRequest().authenticated();
                })
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers
                        // DENY em produção; sameOrigin em dev só porque o console do H2 usa <iframe>.
                        .frameOptions(frame -> {
                            if (devProfile) {
                                frame.sameOrigin();
                            } else {
                                frame.deny();
                            }
                        })
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(devProfile ? CSP_DEV : CSP_PROD))
                        .referrerPolicy(rp -> rp.policy(ReferrerPolicy.SAME_ORIGIN))
                        .permissionsPolicy(pp -> pp
                                .policy("camera=(), microphone=(), geolocation=(), payment=()")));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> allowedOrigins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        // Vazio (ex: env var não definida em prod) = nenhuma origem liberada, por segurança.
        configuration.setAllowedOriginPatterns(allowedOrigins.isEmpty() ? List.of() : allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Custo do BCrypt. 12 em produção/dev (padrão do Spring é 10); os testes
    // baixam isso via application-test.yml para não pagar o custo em cada hash.
    // Hashes antigos em custo 10 continuam válidos — o custo fica gravado no hash.
    @Value("${app.security.bcrypt-strength:12}")
    private int bcryptStrength;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
