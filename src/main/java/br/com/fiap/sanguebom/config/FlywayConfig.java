package br.com.fiap.sanguebom.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    // O nome do método deve ser estritamente "flyway".
    // O Spring Boot monitora esse nome para segurar o Hibernate até a migration acabar.
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        System.out.println("🚀 [FLYWAY] Assumindo o controle e forçando a migration manual!");

        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }
}