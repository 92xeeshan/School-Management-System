package com.schoolms.config;

import com.schoolms.tenant.TenantAwareHikariDataSource;
import com.zaxxer.hikari.HikariConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Builds the runtime datasource explicitly so the app connects as the
 * RLS-enforced role (app_rls). Flyway uses its own credentials (schema owner)
 * configured via spring.flyway.* — see application.yml.
 */
@Configuration
public class DatasourceConfig {

    @Bean
    public DataSource dataSource(Environment env) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("schoolms-rls");
        config.setJdbcUrl(env.getProperty("spring.datasource.url",
                "jdbc:postgresql://localhost:5432/schoolms"));
        config.setUsername(env.getProperty("spring.datasource.username", "app_rls"));
        config.setPassword(env.getProperty("spring.datasource.password", "app_rls"));
        config.setMaximumPoolSize(env.getProperty("spring.datasource.hikari.maximum-pool-size",
                Integer.class, 10));
        config.setConnectionTimeout(30_000);
        return new TenantAwareHikariDataSource(config);
    }
}
