package DAO;

import Model.Customer;
import Config.DB_TukangNow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerDAO {

    public CustomerDAO() {
    }

    public boolean isDuplicate(String phone, String email) throws SQLException {
        boolean duplicate = false;
        String checkSql = "SELECT email FROM customer WHERE REPLACE(REPLACE(nophone, '-', ''), ' ', '') = ? OR email = ? " +
                          "UNION " +
                          "SELECT email FROM vendor WHERE REPLACE(REPLACE(nophone, '-', ''), ' ', '') = ? OR email = ?";
        
        try (Connection conn = DB_TukangNow.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            
            stmt.setString(1, phone);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.setString(4, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    duplicate = true;
                }
            }
        }
        return duplicate;
    }

    public boolean registerCustomer(Customer customer) throws SQLException {
        String insertSql = "INSERT INTO customer (name, email, nophone, password, address, postcode, city, state, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        boolean rowInserted = false;
        
        try (Connection conn = DB_TukangNow.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            
            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getEmail());
            stmt.setString(3, customer.getNophone());
            stmt.setString(4, customer.getPassword());
            stmt.setString(5, customer.getAddress());
            stmt.setString(6, customer.getPostcode());
            stmt.setString(7, customer.getCity());
            stmt.setString(8, customer.getState());
            stmt.setDouble(9, customer.getLatitude());
            stmt.setDouble(10, customer.getLongitude());
            
            rowInserted = stmt.executeUpdate() > 0;
        }
        return rowInserted;
    }
}