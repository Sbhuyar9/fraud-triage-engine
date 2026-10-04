package com.fraudtriage.service;

import com.fraudtriage.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class OrchestratorClient {
    private final RestClient client;

    public OrchestratorClient(@Value("${agent.base-url}") String baseUrl) {
        // Without explicit timeouts the default HTTP client factory has no read timeout, so a
        // hung or slow agent-orchestrator would block the request thread indefinitely.
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(Duration.ofSeconds(3))
                        .withReadTimeout(Duration.ofSeconds(8)));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public OrchestrationResponse orchestrate(TransactionRequest request) {
        return client.post().uri("/orchestrate")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(OrchestrationResponse.class);
    }
}
