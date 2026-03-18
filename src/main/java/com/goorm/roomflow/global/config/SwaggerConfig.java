package com.goorm.roomflow.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
	//swagger info정하기
	private Info info() {
		return new Info()
				.title("API Test")
				.version("1.0.0")
				.description("Practice Swagger UI");
	}

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.components(new Components())
				.info(info());
	}
}
