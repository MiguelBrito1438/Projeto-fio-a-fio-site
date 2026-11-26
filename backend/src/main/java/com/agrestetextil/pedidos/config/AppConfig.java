package com.agrestetextil.pedidos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String origensPermitidas;

    /** Relogio unico da aplicacao (facilita testes de atraso com data fixa). */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Fortaleza"));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origensPermitidas.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
