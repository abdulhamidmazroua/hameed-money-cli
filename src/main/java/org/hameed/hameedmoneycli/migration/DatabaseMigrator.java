package org.hameed.hameedmoneycli.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DatabaseMigrator implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrator.class);

    private final DataSource dataSource;

    private volatile boolean running;

    public DatabaseMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void start() {
        running = true;
        try (Connection conn = dataSource.getConnection()) {
            int current = getVersion(conn);
            int latest = SchemaVersion.LATEST;

            if (current >= latest) {
                return;
            }

            log.info("Database schema version: {}, latest: {} — applying {} migration(s)",
                    current, latest, latest - current);

            for (int v = current + 1; v <= latest; v++) {
                log.info("Applying migration V{}...", v);
                Migrations.ALL.get(v).accept(conn);
                setVersion(conn, v);
                log.info("Migration V{} applied", v);
            }
        } catch (Exception e) {
            throw new RuntimeException("Migration failed", e);
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }

    private int getVersion(Connection conn) {
        try (var stmt = conn.createStatement();
             var rs = stmt.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void setVersion(Connection conn, int version) {
        try (var stmt = conn.createStatement()) {
            stmt.execute("PRAGMA user_version = " + version);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set schema version to " + version, e);
        }
    }

}
