package org.hameed.hameedmoneycli.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class BackupService {

    public Path backup(String outputDir) throws IOException {
        Path dir = Path.of(outputDir);
        Files.createDirectories(dir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path outputFile = dir.resolve("hmc-" + timestamp + ".sqlite");

        Path dbPath = dbPath();

        if (!Files.exists(dbPath)) {
            throw new IllegalStateException("Database not found at " + dbPath);
        }

        try {
            backupViaSqlite3(dbPath, outputFile);
        } catch (Exception e) {
            backupViaCopy(dbPath, outputFile);
        }

        return outputFile;
    }

    private void backupViaSqlite3(Path dbPath, Path outputFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "sqlite3", dbPath.toString(),
                ".backup '" + outputFile + "'"
        );
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        int exit = pb.start().waitFor();
        if (exit != 0) {
            throw new IOException("sqlite3 .backup failed with exit code " + exit);
        }
    }

    private void backupViaCopy(Path dbPath, Path outputFile) throws IOException {
        Files.copy(dbPath, outputFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private static Path dbPath() {
        String home = System.getProperty("user.home");
        return Path.of(home, ".hmc", "hmc.db");
    }
}
