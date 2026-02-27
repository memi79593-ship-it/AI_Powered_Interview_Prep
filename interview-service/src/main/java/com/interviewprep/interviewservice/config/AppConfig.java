package com.interviewprep.interviewservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Application-wide Spring bean configuration.
 */
@Configuration
public class AppConfig {

    /**
     * RestTemplate bean used by AIClientService to call Flask.
     * Declared here so it can be injected and mocked in tests.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
