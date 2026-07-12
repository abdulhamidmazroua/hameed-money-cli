package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.constants.CommandConstants;
import org.hameed.hameedmoneycli.service.ConvertService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.hameed.hameedmoneycli.constants.CommandConstants.*;
import static org.hameed.hameedmoneycli.util.CommandsUtil.*;

@Configuration
@RequiredArgsConstructor
public class ConvertCommands {

    private final ConvertService convertService;

    private static final String INPUT_ARG = "input";
    private static final String OUTPUT_ARG = "output";

    @Bean
    public Command convert() {
        return Command.builder()
                .name("convert")
                .description(CONVERT_COMMAND_DESCRIPTION)
                .help(CONVERT_COMMAND_HELP)
                .options(
                        CommandOption.with().shortName('i').longName(INPUT_ARG).required(false).type(String.class).build(),
                        CommandOption.with().shortName('o').longName(OUTPUT_ARG).required(false).type(String.class).build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String input = argOrOption(ctx, 0, 'i', INPUT_ARG);
                    if (input == null) throw new IllegalArgumentException(CONVERT_INPUT_ARG_ERROR);

                    Path inputPath = Path.of(input);
                    if (!Files.exists(inputPath)) {
                        throw new IllegalArgumentException("File not found: " + input);
                    }

                    String output = argOrOption(ctx, 1, 'o', OUTPUT_ARG, null);
                    Path outputPath = output != null ? Path.of(output)
                            : inputPath.resolveSibling(stripExtension(inputPath) + ".csv");

                    try {
                        ctx.outputWriter().println("Converting " + input + " via LLM...");
                        String csv = convertService.convertToCsv(input);

                        Files.writeString(outputPath, csv, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                        ctx.outputWriter().println("Converted CSV written to " + outputPath.toAbsolutePath());
                    } catch (IOException e) {
                        throw new RuntimeException("Conversion failed: " + e.getMessage(), e);
                    }
                });
    }

    private static String stripExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
