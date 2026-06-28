package org.hameed.hameedmoneycli.service;

import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class BackupService {

    public Path backup(String outputDir) throws IOException, InterruptedException {
        Path dir = Path.of(outputDir);
        Files.createDirectories(dir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path outputFile = dir.resolve("hmc-" + timestamp + ".sql");

        checkDockerComposeAvailable();

        ProcessBuilder pb = new ProcessBuilder(
                "docker", "compose", "exec", "-T", "db",
                "pg_dump", "-U", "hmc-user", "hmc-db"
        );

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "docker compose not found. Make sure Docker is running:\n  docker compose up -d db"
            );
        }

        try (InputStream stdout = process.getInputStream();
             FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
            stdout.transferTo(fos);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error;
            try (InputStream stderr = process.getErrorStream()) {
                error = new String(stderr.readAllBytes());
            }
            throw new IOException("pg_dump via docker compose failed (exit " + exitCode + "): " + error);
        }

        return outputFile;
    }

    private void checkDockerComposeAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "compose", "version").start();
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalStateException("docker compose is not available.");
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "docker compose not found. Install Docker and start the DB:\n  docker compose up -d db"
            );
        }
    }
}
