package org.hameed.hameedmoneycli.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class for tests that boot the full application context.
 * <ul>
 *   <li>Runs against a throwaway database and config under {@code target/}, never the real {@code ~/.hmc}.</li>
 *   <li>Disables the interactive shell so the context starts and returns instead of waiting for input.</li>
 *   <li>Each test runs in a transaction that is rolled back, so tests don't see each other's data.</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.shell.interactive.enabled=false",
        "spring.shell.history.enabled=false",
        // With the shell disabled and no command given, the runner logs a harmless error; hide it.
        "logging.level.org.springframework.shell.core.NonInteractiveShellRunner=OFF"
})
@Transactional
public abstract class IntegrationTest {

    private static final Path HOME = createHome();

    @DynamicPropertySource
    static void isolatedStorage(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + HOME.resolve("hmc.db"));
        registry.add("hmc.config.path", () -> HOME.resolve("config.json").toString());
        registry.add("hmc.report.output-dir", () -> HOME.resolve("reports").toString());
    }

    private static Path createHome() {
        try {
            Path home = Files.createTempDirectory("hmc-test-");
            home.toFile().deleteOnExit();
            return home;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
