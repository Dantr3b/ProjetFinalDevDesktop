package fr.retrosphere.gamevault.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (input != null) {
                PROPERTIES.load(input);
            }
        } catch (IOException exception) {
            // Missing or unreadable configuration should not prevent the app from using safe defaults.
            System.err.println("Impossible de charger application.properties. Valeurs par defaut utilisees.");
        }
    }

    private AppConfig() {
    }

    public static String get(String key, String fallback) {
        return PROPERTIES.getProperty(key, fallback);
    }
}
