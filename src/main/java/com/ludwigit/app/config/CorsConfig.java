package com.ludwigit.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private final AppConfig appConfig;

	public CorsConfig(AppConfig appConfig) {
		this.appConfig = appConfig;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**") // Apply to all endpoints
			.allowedOrigins(appConfig.getWebClientUrl()) // Allow the web application
			.allowedMethods("GET", "POST", "OPTIONS", "HEAD")
			.allowedHeaders("*");
	}

}
