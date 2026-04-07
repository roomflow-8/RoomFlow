package com.goorm.roomflow.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {
	@Bean
	public WebClient webClient() {
		return WebClient.builder()
				.baseUrl("https://api.tosspayments.com/v1/payments")
				.clientConnector(new ReactorClientHttpConnector(
						HttpClient.create()
								.responseTimeout(Duration.ofSeconds(30))  // 타임아웃 설정
				))
				.build();
	}
}
