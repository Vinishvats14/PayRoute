package com.vinu.payroute.transaction.client;

import com.vinu.payroute.transaction.dto.RiskCheckRequest;
import com.vinu.payroute.transaction.dto.RiskCheckResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class RiskClient {

    private final WebClient webClient;

    public RiskClient(
            WebClient.Builder webClientBuilder
    ) {

        this.webClient =
                webClientBuilder
                        .baseUrl("http://localhost:8084")
                        .build();
    }

    public RiskCheckResponse check(
            RiskCheckRequest request
    ) {

        return webClient
                .post()
                .uri("/risk/check")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(
                        RiskCheckResponse.class
                )
                .block();
    }

    public RiskCheckResponse validateTransferCompletion(
            RiskCheckRequest request
    ) {
        return webClient
                .post()
                .uri("/risk/validate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(
                        RiskCheckResponse.class
                )
                .block();
    }

    public void recordDailyTransfer(Long userId, BigDecimal amount) {
        webClient
                .post()
                .uri("/risk/daily/record")
                .bodyValue(Map.of(
                        "userId", userId,
                        "amount", amount
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}