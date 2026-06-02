package fr.retrosphere.gamevault.persistence;

import fr.retrosphere.gamevault.config.AppConfig;
import fr.retrosphere.gamevault.model.Game;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class HibernateUtil {
    private static SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    public static synchronized void initialize() {
        if (sessionFactory != null) {
            return;
        }
        try {
            Files.createDirectories(Path.of("data"));
            Configuration configuration = new Configuration();
            configuration.addAnnotatedClass(Game.class);
            configuration.setProperties(hibernateProperties());
            sessionFactory = configuration.buildSessionFactory();
        } catch (Exception exception) {
            throw new IllegalStateException("Initialisation de la base SQLite impossible", exception);
        }
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            initialize();
        }
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    private static Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.connection.driver_class", "org.sqlite.JDBC");
        properties.put("hibernate.connection.url", AppConfig.get("database.url", "jdbc:sqlite:data/gamevault.db"));
        properties.put("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");
        properties.put("hibernate.hbm2ddl.auto", AppConfig.get("hibernate.hbm2ddl.auto", "update"));
        properties.put("hibernate.show_sql", AppConfig.get("hibernate.show_sql", "false"));
        properties.put("hibernate.format_sql", "true");
        return properties;
    }
}
