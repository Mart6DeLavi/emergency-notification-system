package com.sensa.usermanagementservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Spring Boot backs off its DataSource auto-configuration when a reactive
 * R2DBC ConnectionFactory is present. Liquibase still needs a JDBC DataSource,
 * so it is created explicitly here from spring.datasource.*.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }
}
