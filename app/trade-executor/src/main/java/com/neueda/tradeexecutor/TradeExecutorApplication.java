package com.neueda.tradeexecutor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication()
@EnableScheduling
public class TradeExecutorApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeExecutorApplication.class, args);
    }
}
