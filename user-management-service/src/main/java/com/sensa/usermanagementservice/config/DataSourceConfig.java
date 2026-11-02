package com.sensa.usermanagementservice.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Spring Boot backs off its DataSource auto-configuration when a reactive
 * R2DBC ConnectionFactory is present. Liquibase still needs a JDBC DataSource,
 * so it is created explicitly here from spring.datasource.*.
 *
 * DataSourceProperties maps spring.datasource.url to Hikari's jdbcUrl; binding
 * spring.datasource.* directly to HikariDataSource does not (Hikari has no
 * "url" property), which caused "jdbcUrl is required with driverClassName".
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}
