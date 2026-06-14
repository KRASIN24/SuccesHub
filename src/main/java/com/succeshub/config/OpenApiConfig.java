package com.succeshub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI successHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SuccessHub API")
                        .description("SuccessHub platform REST API")
                        .version("v1"));
    }
}
