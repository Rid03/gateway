package com.api.gateway.limiter;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Optional;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver principalOrIpKeyResolver() {
        return exchange ->
                exchange.getPrincipal()
                        .flatMap(principal -> {
                            if (principal instanceof JwtAuthenticationToken jwt) {
                                // 1) sub (subject)
                                String sub = jwt.getToken().getSubject();
                                if (sub != null && !sub.isBlank()) return Mono.just(sub);

                                // 2) preferred_username
                                String username = jwt.getToken().getClaimAsString("preferred_username");
                                if (username != null && !username.isBlank()) return Mono.just(username);
                            }
                            // 3) fallback → IP
                            return resolveClientIp(exchange);
                        })
                        .switchIfEmpty(resolveClientIp(exchange)); // если principal отсутствует
    }

    private Mono<String> resolveClientIp(ServerWebExchange exchange) {
        // Сначала X-Forwarded-For (если за балансировщиком/ingress)
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // первый IP в списке — клиент
            String ip = xff.split(",")[0].trim();
            if (!ip.isBlank()) return Mono.just(ip);
        }
        // Затем RemoteAddress (может быть null)
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        String ip = Optional.ofNullable(remote)
                .map(InetSocketAddress::getAddress)
                .map(addr -> addr.getHostAddress())
                .orElse("anonymous");
        return Mono.just(ip);
    }
}