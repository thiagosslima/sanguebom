package br.com.fiap.sanguebom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@SpringBootApplication
@ConfigurationPropertiesScan
public class SanguebomApplication {

    public static void main(final String[] args) {
        SpringApplication.run(SanguebomApplication.class, args);
    }

}
