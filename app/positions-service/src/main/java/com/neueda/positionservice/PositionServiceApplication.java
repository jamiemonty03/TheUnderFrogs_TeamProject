package com.neueda.positionservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication()
@MapperScan("com.neueda.positionservice.repositories")
public class PositionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PositionServiceApplication.class, args);
    }
}
