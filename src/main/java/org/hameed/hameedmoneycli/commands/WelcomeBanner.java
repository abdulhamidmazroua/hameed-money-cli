package org.hameed.hameedmoneycli.commands;

import jakarta.annotation.PostConstruct;
import org.hameed.hameedmoneycli.util.CommandsUtil;
import org.springframework.stereotype.Component;

@Component
public class WelcomeBanner {

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
            System.out.println("  \033[36m\u2551              HameedMoneyCLI                      \u2551");
            System.out.println("  \033[36m\u2551         Your Personal Finance Manager            \u2551");
            System.out.println("  \033[36m\u255A\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255D\033[0m");
            System.out.print(CommandsUtil.guidelines());
        });
    }
}
