package com.neueda.orderservice;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication()
public class OrderServiceApplication {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(entry -> {
            String key = entry.getKey();
            String value = entry.getValue();
            if (System.getProperty(key) == null && System.getenv(key) == null) {
                System.setProperty(key, value);
            }
        });
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
