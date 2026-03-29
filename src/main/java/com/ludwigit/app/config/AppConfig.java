package com.ludwigit.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "app")
@EnableConfigurationProperties
@Validated
@Data
public class AppConfig {

	private String baseUrl;

	private String webClientUrl;

}
