package com.api.gateway.logging;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter extends AbstractGatewayFilterFactory<LoggingFilter.Config> {

    public LoggingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            System.out.println("[Gateway LOG] Request: " + request.getMethod() + " " + request.getURI());
            long startTime = System.currentTimeMillis();

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("[Gateway LOG] Response Status: " + exchange.getResponse().getStatusCode() +
                        ", Duration: " + duration + "ms");
            }));
        };
    }

    public static class Config {
        // можно добавить уровни логирования, фильтры по пути и т.д.
    }
}