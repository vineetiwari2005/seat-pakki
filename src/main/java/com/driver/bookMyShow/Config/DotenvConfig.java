package com.driver.bookMyShow.Config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads environment variables from .env file
 * This runs before Spring Boot starts, ensuring all env vars are available
 */
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();

        List<String> searchDirectories = List.of(
                "./",
                "./Book-My-Show",
                "../Book-My-Show"
        );

        Dotenv loadedDotenv = null;
        String loadedFrom = null;

        for (String directory : searchDirectories) {
            try {
                Dotenv candidate = Dotenv.configure()
                        .directory(directory)
                        .ignoreIfMissing()
                        .load();

                if (candidate.entries().iterator().hasNext()) {
                    loadedDotenv = candidate;
                    loadedFrom = directory;
                    break;
                }
            } catch (Exception ignored) {
            }
        }

        if (loadedDotenv == null) {
            System.err.println("⚠️  No .env file found in expected directories. Using system env / application.properties defaults.");
            return;
        }

        Map<String, Object> envMap = new HashMap<>();
        loadedDotenv.entries().forEach(entry -> envMap.put(entry.getKey(), entry.getValue()));

        environment.getPropertySources()
                .addFirst(new MapPropertySource("dotenvProperties", envMap));

        System.out.println("✅ Loaded environment variables from .env file");
        System.out.println("   - loaded from: " + loadedFrom);
        System.out.println("   - DB_USERNAME: " + loadedDotenv.get("DB_USERNAME", "not set"));
        System.out.println("   - STRIPE keys: " + (loadedDotenv.get("STRIPE_SECRET_KEY") != null ? "loaded" : "not set"));
        System.out.println("   - TWOFACTOR_API_KEY: " + (loadedDotenv.get("TWOFACTOR_API_KEY") != null ? "loaded" : "not set"));
        System.out.println("   - FAST2SMS_API_KEY: " + (loadedDotenv.get("FAST2SMS_API_KEY") != null ? "loaded" : "not set"));
    }
}
