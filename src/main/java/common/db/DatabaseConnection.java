package common.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseConnection {

    private static final String CONFIG_FILE = "database.properties";
    private static final Properties PROPERTIES = loadProperties();

    public static Connection getConnection() {
        String url = buildJdbcUrl();

        String username = getRequiredConfigValue(
                "DB_USERNAME",
                "db.username"
        );

        String password = getRequiredConfigValue(
                "DB_PASSWORD",
                "db.password"
        );

        try {
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            Connection connection = DriverManager.getConnection(url, username, password);
            configureOracleSession(connection);
            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException(buildConnectionErrorMessage(url, username), e);
        }
    }

    private static void configureOracleSession(Connection connection) {
        if (connection == null) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            // Tránh tình trạng UI đứng lâu khi UPDATE/DELETE gặp row/table đang bị lock.
            // Oracle sẽ trả ORA-00054 sau tối đa 3 giây thay vì chờ vô hạn.
            statement.execute("ALTER SESSION SET DML_LOCK_TIMEOUT = 3");
        } catch (SQLException e) {
            System.err.println("[DB] Cannot set DML_LOCK_TIMEOUT: " + e.getMessage());
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

    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return con != null && !con.isClosed();
        } catch (Exception e) {
            System.err.println("[DB] Test connection failed: " + e.getMessage());
            return false;
        }
    }

    public static String getCurrentJdbcUrlForLog() {
        return buildJdbcUrl();
    }

    public static String getCurrentUsernameForLog() {
        return getRequiredConfigValue("DB_USERNAME", "db.username");
    }

    private static String buildJdbcUrl() {
        /*
         * Ưu tiên 1:
         * Nếu có DB_URL hoặc db.url thì dùng trực tiếp.
         * Ví dụ:
         * db.url=jdbc:oracle:thin:@10.0.216.238:1521:orcl
         */
        String directUrl = getConfigValue("DB_URL", "db.url");

        if (directUrl != null && !directUrl.isBlank()) {
            return directUrl.trim();
        }

        /*
         * Ưu tiên 2:
         * Tự build URL từ host/port/SID hoặc service.
         */
        String host = getRequiredConfigValue("DB_HOST", "db.host");
        String port = getRequiredConfigValue("DB_PORT", "db.port");
        String connectionType = getConfigValue("DB_CONNECTION_TYPE", "db.connection_type");

        if (connectionType == null || connectionType.isBlank()) {
            connectionType = "SID";
        }

        connectionType = connectionType.trim().toUpperCase();

        if ("SERVICE".equals(connectionType)) {
            String service = getRequiredConfigValue("DB_SERVICE", "db.service");
            return "jdbc:oracle:thin:@//" + host + ":" + port + "/" + service;
        }

        String sid = getRequiredConfigValue("DB_SID", "db.sid");
        return "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream inputStream = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (inputStream == null) {
                throw new IllegalStateException(
                        "Cannot find " + CONFIG_FILE + " on the classpath. "
                        + "Check src/main/resources/" + CONFIG_FILE + " and rebuild the project."
                );
            }

            properties.load(inputStream);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot load database configuration from " + CONFIG_FILE,
                    e
            );
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
                    + "Set environment variable " + envKey
                    + " or define " + propertyKey
                    + " in " + CONFIG_FILE + "."
            );
        }

        return value.trim();
    }

    private static String buildConnectionErrorMessage(String url, String username) {
        return "Cannot connect to Oracle database. "
                + "DB URL=" + url
                + ", DB username=" + username
                + ". Check DataGrip Host/Port/SID/User/Password and "
                + CONFIG_FILE + ".";
    }
}
