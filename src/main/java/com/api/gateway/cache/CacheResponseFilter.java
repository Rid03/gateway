package com.api.gateway.cache;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class CacheResponseFilter extends AbstractGatewayFilterFactory<CacheResponseFilter.Config> {

    private final ReactiveStringRedisTemplate redisTemplate;

    public CacheResponseFilter(ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
                return chain.filter(exchange);
            }

            String key = "cache:" + exchange.getRequest().getURI();

            return redisTemplate.opsForValue().get(key)
                    .flatMap(cachedBody -> {
                        exchange.getResponse().getHeaders().add("X-Cache", "HIT");
                        DataBuffer buffer = exchange.getResponse()
                                .bufferFactory()
                                .wrap(cachedBody.getBytes());
                        return exchange.getResponse().writeWith(Flux.just(buffer));
                    })
                    .switchIfEmpty(cacheAndForward(exchange, chain, key, config.getTtl()));
        };
    }

    private Mono<Void> cacheAndForward(ServerWebExchange exchange,
                                       org.springframework.cloud.gateway.filter.GatewayFilterChain chain,
                                       String key,
                                       Duration ttl) {
        ServerHttpResponse originalResponse = exchange.getResponse();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;

                    Flux<DataBuffer> cacheFlux = fluxBody.map(dataBuffer -> {
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        redisTemplate.opsForValue().set(key, new String(content), ttl).subscribe();
                        return dataBuffer;
                    });

                    exchange.getResponse().getHeaders().add("X-Cache", "MISS");
                    return super.writeWith(cacheFlux);
                }
                return super.writeWith(body);
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    public static class Config {
        private Duration ttl = Duration.ofSeconds(30);

        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
    }
}