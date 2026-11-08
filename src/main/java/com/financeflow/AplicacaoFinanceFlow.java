package com.financeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AplicacaoFinanceFlow {

    public static void main(String[] args) {
        SpringApplication.run(AplicacaoFinanceFlow.class, args);
    }
}
