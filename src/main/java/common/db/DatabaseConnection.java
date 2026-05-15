package common.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static final String CONFIG_FILE = "database.properties";
    private static final Properties PROPERTIES = loadProperties();

    public static Connection getConnection() {
        String url = getRequiredConfigValue("DB_URL", "db.url");
        String username = getRequiredConfigValue("DB_USERNAME", "db.username");
        String password = getRequiredConfigValue("DB_PASSWORD", "db.password");

        try {
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new IllegalStateException(buildConnectionErrorMessage(url, username), e);
        }
    }

    public static void closeConnection(Connection c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = DatabaseConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException(
                        "Cannot find " + CONFIG_FILE + " on the classpath. "
                        + "Check src/main/resources/" + CONFIG_FILE + " and rebuild the project."
                );
            }
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load database configuration from " + CONFIG_FILE, e);
        }
        return properties;
    }

    private static String getConfigValue(String envKey, String propertyKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return PROPERTIES.getProperty(propertyKey);
    }

    private static String getRequiredConfigValue(String envKey, String propertyKey) {
        String value = getConfigValue(envKey, propertyKey);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing database configuration for " + propertyKey + ". "
                    + "Set environment variable " + envKey + " or define " + propertyKey + " in " + CONFIG_FILE + ". "
                    + "Check local Oracle, " + CONFIG_FILE + ", and DB_URL/DB_USERNAME/DB_PASSWORD environment variables."
            );
        }
        return value.trim();
    }

    private static String buildConnectionErrorMessage(String url, String username) {
        return "Cannot connect to Oracle database. "
                + "DB URL=" + url + ", DB username=" + username + ". "
                + "Check local Oracle, " + CONFIG_FILE + ", and DB_URL/DB_USERNAME/DB_PASSWORD environment variables.";
    }
}
