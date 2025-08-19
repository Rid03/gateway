package com.api.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final WebClient webClient = WebClient.create("http://keycloak:8080/realms/myrealm/protocol/openid-connect");

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@RequestParam String refreshToken) {
        return webClient.post()
                .uri("/logout")
                .bodyValue("client_id=my-client&client_secret=my-secret&refresh_token=" + refreshToken)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .retrieve()
                .toBodilessEntity();
    }
}