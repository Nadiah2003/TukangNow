package DAO;

import Config.ConnectionManager;
import Model.Vendor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VendorDAO {

    protected Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public boolean checkAvailability(String email, String phoneFormatted) throws SQLException {
        boolean exists = false;

        String checkSql = "SELECT email FROM customer WHERE email = ? OR REPLACE(REPLACE(nophone, '-', ''), ' ', '') = ? "
                + "UNION "
                + "SELECT email FROM vendor WHERE email = ? OR REPLACE(REPLACE(nophone, '-', ''), ' ', '') = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(checkSql)) {

            preparedStatement.setString(1, safe(email));
            preparedStatement.setString(2, safe(phoneFormatted));
            preparedStatement.setString(3, safe(email));
            preparedStatement.setString(4, safe(phoneFormatted));

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    exists = true;
                }
            }
        }

        return exists;
    }

    public boolean registerVendor(Vendor vendor) throws SQLException {
        boolean rowInserted = false;

        String insertSql = "INSERT INTO vendor "
                + "(name, email, nophone, address, postcode, city, state, country, password, doc_path, profile_path, status, role, latitude, longitude) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'Malaysia', ?, ?, ?, 'pending', 'vendor', ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {

            preparedStatement.setString(1, safe(vendor.getName()));
            preparedStatement.setString(2, safe(vendor.getEmail()));
            preparedStatement.setString(3, safe(vendor.getNophone()));
            preparedStatement.setString(4, safe(vendor.getAddress()));
            preparedStatement.setString(5, safe(vendor.getPostcode()));
            preparedStatement.setString(6, safe(vendor.getCity()));
            preparedStatement.setString(7, safe(vendor.getState()));
            preparedStatement.setString(8, safe(vendor.getPassword()));
            preparedStatement.setString(9, safe(vendor.getDocPath()));
            preparedStatement.setString(10, safe(vendor.getProfilePath()));
            preparedStatement.setDouble(11, vendor.getLatitude());
            preparedStatement.setDouble(12, vendor.getLongitude());

            rowInserted = preparedStatement.executeUpdate() > 0;
        }

        return rowInserted;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}