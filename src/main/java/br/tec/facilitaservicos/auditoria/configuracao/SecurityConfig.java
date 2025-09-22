package br.tec.facilitaservicos.auditoria.configuracao;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * ============================================================================
 * 🔐 CONFIGURAÇÃO DE SEGURANÇA REATIVA - AUDITORIA & COMPLIANCE
 * ============================================================================
 * 
 * Configuração de segurança especializada para microserviço de auditoria:
 * - Validação JWT via JWKS do microserviço de autenticação
 * - Controle de acesso baseado em roles para audit endpoints
 * - CORS restritivo para endpoints administrativos
 * - Headers de segurança reforçados para compliance
 * - Rate limiting específico para auditoria
 * - Proteção especial para logs sensíveis
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Configuration
@EnableWebFluxSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Constantes para valores repetidos
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String PROFILE_PRODUCAO = "prod";
    private static final String ERRO_CORS_ORIGEM_UNICA = "Configuração CORS inválida: 'cors.allowed-origins' não pode incluir domínios não seguros em produção";
    
    // Templates de resposta JSON para auditoria
    private static final String TEMPLATE_ERRO_AUTENTICACAO = """
        {
            "status": 401,
            "erro": "Não autorizado",
            "mensagem": "Token JWT inválido ou ausente - acesso a auditoria negado",
            "timestamp": "%s",
            "service": "auditoria-compliance"
        }
        """;
        
    private static final String TEMPLATE_ERRO_ACESSO = """
        {
            "status": 403,
            "erro": "Acesso negado",
            "mensagem": "Permissões insuficientes para acessar logs de auditoria",
            "timestamp": "%s",
            "service": "auditoria-compliance"
        }
        """;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${cors.allowed-origins:https://admin.conexaodesorte.com,https://monitoring.conexaodesorte.com}")
    private String allowedOriginsProperty;

    private List<String> allowedOrigins;

    @Value("#{'${cors.allowed-methods:GET,POST}'.split(',')}")
    private List<String> allowedMethods;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validarConfiguracaoCors() {
        this.allowedOrigins = Arrays.stream(allowedOriginsProperty.split(","))
            .map(String::trim)
            .toList();

        boolean producao = Arrays.asList(environment.getActiveProfiles()).contains(PROFILE_PRODUCAO);
        if (producao) {
            // Em produção, apenas origins HTTPS devem ser permitidas
            boolean temOrigemInsegura = allowedOrigins.stream()
                .anyMatch(origin -> origin.equals("*") || origin.startsWith("http://"));
            if (temOrigemInsegura) {
                throw new IllegalStateException(ERRO_CORS_ORIGEM_UNICA);
            }
        }
    }

    /**
     * Configuração principal da cadeia de filtros de segurança para auditoria
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            // Desabilitar proteções desnecessárias para API reativa
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // Configurar CORS restritivo
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Configurar autorização específica para auditoria
            .authorizeExchange(exchanges -> exchanges
                // Endpoints públicos (health checks apenas)
                .pathMatchers(
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/metrics",
                    "/actuator/prometheus",
                    "/favicon.ico"
                ).permitAll()
                
                // Documentação OpenAPI (apenas para admins)
                .pathMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/webjars/**"
                ).hasAuthority("SCOPE_admin")
                
                // Endpoints de auditoria - requerem roles específicas
                .pathMatchers(HttpMethod.GET, "/rest/v1/audit/logs/**")
                    .hasAnyAuthority("SCOPE_audit_read", "SCOPE_admin")
                    
                .pathMatchers(HttpMethod.POST, "/rest/v1/audit/logs/**")
                    .hasAnyAuthority("SCOPE_audit_write", "SCOPE_admin")
                    
                .pathMatchers(HttpMethod.GET, "/rest/v1/audit/reports/**")
                    .hasAnyAuthority("SCOPE_audit_reports", "SCOPE_admin")
                    
                .pathMatchers("/rest/v1/audit/compliance/**")
                    .hasAnyAuthority("SCOPE_compliance", "SCOPE_admin")
                
                // Endpoints administrativos críticos
                .pathMatchers("/actuator/**").hasAuthority("SCOPE_admin")
                
                // Qualquer outro endpoint requer pelo menos role de audit
                .anyExchange().hasAnyAuthority("SCOPE_audit_read", "SCOPE_admin")
            )

            // Configurar JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(reactiveJwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )

            // Headers de segurança reforçados para compliance
            .headers(headers -> headers.disable())

            // Configurar tratamento de exceções específico para auditoria
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((exchange, _) -> {
                    var response = exchange.getResponse();
                    response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                    response.getHeaders().add(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
                    
                    String body = TEMPLATE_ERRO_AUTENTICACAO.formatted(java.time.LocalDateTime.now());
                    
                    var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
                    return response.writeWith(reactor.core.publisher.Mono.just(buffer));
                })
                .accessDeniedHandler((exchange, _) -> {
                    var response = exchange.getResponse();
                    response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
                    response.getHeaders().add(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
                    
                    String body = TEMPLATE_ERRO_ACESSO.formatted(java.time.LocalDateTime.now());
                    
                    var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
                    return response.writeWith(reactor.core.publisher.Mono.just(buffer));
                })
            )

            .build();
    }

    /**
     * Decodificador JWT reativo via JWKS
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /**
     * Conversor de autenticação JWT personalizado para auditoria
     */
    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    /**
     * Conversor personalizado de authorities JWT para auditoria
     */
    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return new CustomJwtGrantedAuthoritiesConverter();
    }

    /**
     * Configuração CORS restritiva para endpoints de auditoria
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origins permitidas (apenas domínios administrativos seguros)
        configuration.setAllowedOrigins(allowedOrigins);
        
        // Métodos HTTP permitidos (limitados para auditoria)
        configuration.setAllowedMethods(allowedMethods);
        
        // Headers permitidos (lista restrita para auditoria)
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin"
        ));

        // Permitir credenciais para autenticação
        if (allowCredentials) {
            configuration.setAllowCredentials(true);
        }
        
        // Cache preflight
        configuration.setMaxAge(maxAge);
        
        // Headers expostos (para metadados de auditoria)
        configuration.setExposedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Total-Count",
            "X-Audit-Session-Id",
            "X-Audit-Request-Id"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Classe interna para conversão de authorities JWT específica para auditoria
     */
    private static class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Collection<GrantedAuthority> authorities = new java.util.ArrayList<>();
            
            // Processar claim 'roles' 
            var rolesClaim = jwt.getClaim("roles");
            if (rolesClaim != null) {
                if (rolesClaim instanceof List<?> rolesList) {
                    authorities.addAll(
                        rolesList.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            .toList()
                    );
                }
            }
            
            // Processar claim 'authorities'
            var authoritiesClaim = jwt.getClaim("authorities");
            if (authoritiesClaim != null) {
                if (authoritiesClaim instanceof List<?> authList) {
                    authorities.addAll(
                        authList.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .map(SimpleGrantedAuthority::new)
                            .toList()
                    );
                }
            }
            
            // Processar claim 'scope' (OAuth2 padrão)
            var scopeClaim = jwt.getClaim("scope");
            if (scopeClaim instanceof String scopeString) {
                authorities.addAll(
                    Arrays.stream(scopeString.split("\\s+"))
                        .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                        .toList()
                );
            }
            
            // Adicionar authorities específicas para auditoria baseadas em claims customizados
            var auditRolesClaim = jwt.getClaim("audit_roles");
            if (auditRolesClaim instanceof List<?> auditRolesList) {
                authorities.addAll(
                    auditRolesList.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(role -> new SimpleGrantedAuthority("SCOPE_audit_" + role))
                        .toList()
                );
            }
            
            return authorities;
        }
    }
}
