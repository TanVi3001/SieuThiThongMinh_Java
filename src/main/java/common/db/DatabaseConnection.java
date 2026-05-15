package common.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConnection {
    private static final String CONFIG_FILE = "database.properties";
    private static final Properties PROPERTIES = loadProperties();

    public static Connection getConnection() {
        Connection c = null;
        try {
            // Đăng ký Oracle Driver
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());

            // Ưu tiên biến môi trường để dễ đổi cấu hình theo từng máy,
            // sau đó dùng giá trị mặc định trong database.properties.
            String url = getConfigValue("DB_URL", "db.url");
            String username = getConfigValue("DB_USERNAME", "db.username");
            String password = getConfigValue("DB_PASSWORD", "db.password");

            // Thực hiện kết nối
            c = DriverManager.getConnection(url, username, password);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return c;
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
            if (inputStream != null) {
                properties.load(inputStream);
            }
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

}
