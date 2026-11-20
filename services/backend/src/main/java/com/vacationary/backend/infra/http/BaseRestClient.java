package com.vacationary.backend.infra.http;

import java.time.Duration;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public abstract class BaseRestClient {

	protected final RestClient restClient;

	protected BaseRestClient(String baseUrl, String apiKey, RestClient restClient) {
		String key = apiKey == null ? "" : apiKey;
		this.restClient = restClient != null ? restClient : defaultClient(baseUrl, key);
	}

	protected static RestClient defaultClient(String baseUrl, String apiKey) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(10));
		factory.setReadTimeout(Duration.ofSeconds(45));
		RestClient.Builder builder = RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(factory);
		if (apiKey != null && !apiKey.isBlank()) {
			builder.defaultHeader("Authorization", "Bearer " + apiKey);
		}
		return builder.build();
	}
}
