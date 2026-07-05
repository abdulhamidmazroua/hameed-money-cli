package org.hameed.hameedmoneycli;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.shell.jline.PromptProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableJpaAuditing
public class HameedMoneyCliApplication {

    public static void main(String[] args) {
        Path dbDir = Path.of(System.getProperty("user.home"), ".hmc");
        try {
            Files.createDirectories(dbDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create database directory " + dbDir, e);
        }
        SpringApplication.run(HameedMoneyCliApplication.class, args);
    }

    @Bean
    public PromptProvider promptProvider() {
        return () -> new AttributedString("hmc:> ", AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
    }
}
