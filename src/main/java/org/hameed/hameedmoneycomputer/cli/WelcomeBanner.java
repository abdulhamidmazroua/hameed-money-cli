package org.hameed.hameedmoneycomputer.cli;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.ledger.SetupService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WelcomeBanner {

    private final SetupService setupService;

    @PostConstruct
    public void scheduleBanner() {
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            System.out.println();
            System.out.println("  \033[36m\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2555");
            System.out.println("  \033[36m\u2551              Hameed Money Computer               \u2551");
            System.out.println("  \033[36m\u2551         Your Personal Finance Manager            \u2551");
            System.out.println("  \033[36m\u255A\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255D\033[0m");
            System.out.print(CommandsUtil.guidelines());
            if (!setupService.isSetUp()) {
                System.out.println();
                System.out.println("  \033[33mFirst run:\033[0m choose your base currency with  "
                        + "\033[37msetup --currency <CUR>\033[0m  (any ISO 4217 code).");
            }
        });
    }
}
