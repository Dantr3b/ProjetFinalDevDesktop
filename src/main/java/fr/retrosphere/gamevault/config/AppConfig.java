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
            throw new IllegalStateException("Impossible de charger application.properties", exception);
        }
    }

    private AppConfig() {
    }

    public static String get(String key, String fallback) {
        return PROPERTIES.getProperty(key, fallback);
    }
}
