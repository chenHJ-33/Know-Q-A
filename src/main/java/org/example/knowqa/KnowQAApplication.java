package org.example.knowqa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.Transactional;

@SpringBootApplication
@Transactional
public class KnowQAApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowQAApplication.class, args);
    }

}
