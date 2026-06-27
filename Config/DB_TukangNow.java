package Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB_TukangNow {

    private static final String HOST = "127.0.0.1";
    private static final String PORT = "3306";
    private static final String DBNAME = "tukangnow_db"; 
    private static final String USER = "root";
    private static final String PASSWORD = "admin"; 

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DBNAME 
            + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Ralat: Driver MySQL tidak ditemui!");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}