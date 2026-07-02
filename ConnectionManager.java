package Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {
    private static final String URL = "jdbc:mysql://localhost:3306/s72009_tukangnow_db";
    private static final String DRIVERNAME = "com.mysql.jdbc.Driver";
    private static final String USERNAME = "s72009";
    private static final String PASSWORD = "Nadiah2205.";
    private static Connection conn;

    public static Connection getConnection() {
        try {
            Class.forName(DRIVERNAME);
            try{
                conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            }
            catch(SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
        catch(ClassNotFoundException ex) {
            System.out.println(ex.getMessage()+" not found!");
        }
        return conn;
    }
}