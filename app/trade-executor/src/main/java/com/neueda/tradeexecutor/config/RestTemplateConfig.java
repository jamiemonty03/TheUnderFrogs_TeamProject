package com.neueda.tradeexecutor.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.neueda.tradeexecutor.services.ServiceTokenProvider;
import com.neueda.tradeexecutor.config.ServiceTokenInterceptor;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, ServiceTokenProvider serviceTokenProvider) {
        return builder
                .additionalInterceptors(new ServiceTokenInterceptor(serviceTokenProvider))
                .build();
    }
}
