package com.zizaizhiwu.vocabulary;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zizaizhiwu.vocabulary.mapper")
public class VocabularyApplication {

    public static void main(String[] args) {
        SpringApplication.run(VocabularyApplication.class, args);
    }
}
