package com.api.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
public class SecurityConfig {

//    @Bean
//    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//        return http
//                .authorizeExchange(exchanges -> exchanges
//                        .pathMatchers("/api/public/**").permitAll()
//                        .anyExchange().authenticated()
//                )
//                .oauth2Login()  // Аутентификация через Keycloak
//                .and()
//                .oauth2ResourceServer(server -> server
//                        .jwt()  // Валидация JWT
//                )
//                .csrf().disable()
//                .build();
//    }
}
