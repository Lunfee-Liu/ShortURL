package com.example.shorturl.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.shorturl.mapper")
public class MyBatisConfig {
}
