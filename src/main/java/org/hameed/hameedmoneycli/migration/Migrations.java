package org.hameed.hameedmoneycli.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import java.util.function.Consumer;

public final class Migrations {

    private static final Logger log = LoggerFactory.getLogger(Migrations.class);

    public static final Map<Integer, Consumer<Connection>> ALL = Map.of(
            1, Migrations::v1SeedData
    );

    private Migrations() {
    }

    private static void v1SeedData(Connection conn) {
        String sql = loadResource("/db/migration/v001_seed_data.sql");
        String[] statements = sql.split(";\\s*\\n");
        int count = 0;
        try {
            for (String stmt : statements) {
                String trimmed = stmt.trim();
                if (trimmed.isBlank()) continue;
                try (Statement s = conn.createStatement()) {
                    s.execute(trimmed);
                }
                count++;
            }
            log.info("V1: executed {} statements from v001_seed_data.sql", count);
        } catch (Exception e) {
            throw new RuntimeException("V1 migration failed at statement " + (count + 1), e);
        }
    }

    private static String loadResource(String path) {
        try (InputStream is = Migrations.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + path);
            }
            return new String(is.readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load resource: " + path, e);
        }
    }
}
