package com.api.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            Converter<Jwt, ? extends Mono<? extends org.springframework.security.authentication.AbstractAuthenticationToken>> jwtAuthConverter
    ) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> {}) // по умолчанию
                .authorizeExchange(auth -> auth
                        // открытые эндпоинты
                        .pathMatchers("/public/**").permitAll()
                        .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll() // при желании можно ограничить hasRole("ADMIN")
                        .pathMatchers("/eureka/**").permitAll()
                        // защищённые
                        .pathMatchers("/admin/**").hasRole("ADMIN")
                        .pathMatchers("/users/**").hasAnyRole("USER", "ADMIN")
                        // всё остальное — только аутентифицированным
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                )
                .build();
    }

    /**
     * REACTIVE конвертер Jwt -> Mono<AbstractAuthenticationToken>
     * Используем адаптер для стандартного JwtAuthenticationConverter.
     */
    @Bean
    public Converter<Jwt, ? extends Mono<? extends org.springframework.security.authentication.AbstractAuthenticationToken>> jwtAuthConverter() {
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(keycloakAuthoritiesConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(delegate);
    }

    /**
     * Достаём роли из Keycloak:
     * - realm_access.roles
     * - resource_access.{client}.roles
     * Возвращаем коллекцию GrantedAuthority с префиксом ROLE_.
     */
    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> keycloakAuthoritiesConverter() {
        return jwt -> {
            Set<String> roles = new HashSet<>();

            // realm_access.roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                Object ra = realmAccess.get("roles");
                if (ra instanceof Collection<?> col) {
                    col.forEach(r -> roles.add(String.valueOf(r)));
                }
            }

            // resource_access.*.roles
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                resourceAccess.values().forEach(val -> {
                    if (val instanceof Map<?, ?> m) {
                        Object rr = m.get("roles");
                        if (rr instanceof Collection<?> col) {
                            col.forEach(r -> roles.add(String.valueOf(r)));
                        }
                    }
                });
            }

            // маппим в ROLE_*
            return roles.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> new SimpleGrantedAuthority("ROLE_" + s.toUpperCase()))
                    .collect(Collectors.toSet());
        };
    }
}