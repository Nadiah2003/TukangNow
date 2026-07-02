package DAO;

import Model.Customer;
import Config.ConnectionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerDAO {

    public CustomerDAO() {
    }

    public boolean isDuplicate(String phone, String email) throws SQLException {
        boolean duplicate = false;

        String checkSql = "SELECT email FROM customer WHERE REPLACE(REPLACE(nophone, '-', ''), ' ', '') = ? OR email = ? "
                + "UNION "
                + "SELECT email FROM vendor WHERE REPLACE(REPLACE(nophone, '-', ''), ' ', '') = ? OR email = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {

            stmt.setString(1, safe(phone));
            stmt.setString(2, safe(email));
            stmt.setString(3, safe(phone));
            stmt.setString(4, safe(email));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    duplicate = true;
                }
            }
        }

        return duplicate;
    }

    public boolean registerCustomer(Customer customer) throws SQLException {
        String insertSql = "INSERT INTO customer "
                + "(name, email, nophone, password, address, postcode, city, state, latitude, longitude) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        boolean rowInserted = false;

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {

            stmt.setString(1, safe(customer.getName()));
            stmt.setString(2, safe(customer.getEmail()));
            stmt.setString(3, safe(customer.getNophone()));
            stmt.setString(4, safe(customer.getPassword()));
            stmt.setString(5, safe(customer.getAddress()));
            stmt.setString(6, safe(customer.getPostcode()));
            stmt.setString(7, safe(customer.getCity()));
            stmt.setString(8, safe(customer.getState()));
            stmt.setDouble(9, customer.getLatitude());
            stmt.setDouble(10, customer.getLongitude());

            rowInserted = stmt.executeUpdate() > 0;
        }

        return rowInserted;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}