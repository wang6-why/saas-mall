package com.saas.mall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.saas.mall.mapper")
public class SaasMallApplication {
    public static void main(String[] args) {
        SpringApplication.run(SaasMallApplication.class, args);
    }
}
