package common.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    
    public static Connection getConnection() {
        Connection c = null;
        try {
            // Đăng ký Oracle Driver
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            
            // Cấu hình thông số kết nối
            // "localhost" là máy của bạn, "1521" là cổng mặc định, "orcl" là tên database
//            String url = "jdbc:oracle:thin:@localhost:1521:orcl"; 
            String url = "jdbc:oracle:thin:@10.0.250.60:1521:orcl";
            // Thay đổi Username và Password theo đúng tài khoản Oracle 
            String username = "system"; 
            String password = "Admin123"; 
            
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

}