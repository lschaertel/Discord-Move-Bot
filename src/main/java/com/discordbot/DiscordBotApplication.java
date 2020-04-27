package com.discordbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Spring Boot Application runner. This resides in the main package, so that all Types in sub-packages are
 * automatically wired to work as expected.
 */
@SpringBootApplication
public class DiscordBotApplication {

    /**
     * The main method which is started by <code>mvn spring-boot:run</code>.
     *
     * @param args The CLI arguments (ignored).
     */
    public static void main(String[] args) {
        SpringApplication.run(DiscordBotApplication.class, args);
    }

}
