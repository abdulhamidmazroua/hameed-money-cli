package org.hameed.hameedmoneycli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class HameedMoneyCliApplication {

    public static void main(String[] args) {
        SpringApplication.run(HameedMoneyCliApplication.class, args);
    }

}
