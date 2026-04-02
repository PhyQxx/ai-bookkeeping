package com.aibookkeeping;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.aibookkeeping.mapper")
public class AibookkeepingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AibookkeepingApplication.class, args);
    }
}
