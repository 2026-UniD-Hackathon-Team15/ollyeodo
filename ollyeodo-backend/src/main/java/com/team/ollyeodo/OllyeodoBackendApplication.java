package com.team.ollyeodo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OllyeodoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OllyeodoBackendApplication.class, args);
    }

}
