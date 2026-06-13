package com.bingli.lihuaAgent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@MapperScan("com.bingli.lihuaAgent.mapper")
@SpringBootApplication
public class LihuaAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LihuaAgentApplication.class, args);
    }

}
