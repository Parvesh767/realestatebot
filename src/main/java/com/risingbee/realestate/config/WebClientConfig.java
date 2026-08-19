package com.risingbee.realestate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

	 @Bean
	    public WebClient webClient() {
	        return WebClient.builder().build();
	    }
	 
	 
	 
	 @Bean
	    public RestClient.Builder restClientBuilder() {
	        return RestClient.builder();
	    }
	 
	 
}
