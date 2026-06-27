package DAO;

import Config.DB_TukangNow;
import Model.Vendor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VendorDAO {

    protected Connection getConnection() throws SQLException {
        Connection connection = DB_TukangNow.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public boolean checkAvailability(String email, String phoneFormatted) throws SQLException {
        boolean exists = false;

        String checkSql = "SELECT email FROM customer WHERE email = ? OR REPLACE(REPLACE(nophone, '-', ''), ' ', '') = ? " +
                          "UNION " +
                          "SELECT email FROM vendor WHERE email = ? OR REPLACE(REPLACE(nophone, '-', ''), ' ', '') = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(checkSql)) {

            preparedStatement.setString(1, email);
            preparedStatement.setString(2, phoneFormatted);
            preparedStatement.setString(3, email);
            preparedStatement.setString(4, phoneFormatted);

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

        String insertSql = "INSERT INTO vendor " +
                "(name, email, nophone, address, postcode, city, state, country, password, doc_path, profile_path, status, role, latitude, longitude) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'Malaysia', ?, ?, ?, 'pending', 'vendor', ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {

            preparedStatement.setString(1, vendor.getName());
            preparedStatement.setString(2, vendor.getEmail());
            preparedStatement.setString(3, vendor.getNophone());
            preparedStatement.setString(4, vendor.getAddress());
            preparedStatement.setString(5, vendor.getPostcode());
            preparedStatement.setString(6, vendor.getCity());
            preparedStatement.setString(7, vendor.getState());
            preparedStatement.setString(8, vendor.getPassword());
            preparedStatement.setString(9, vendor.getDocPath());
            preparedStatement.setString(10, vendor.getProfilePath());
            preparedStatement.setDouble(11, vendor.getLatitude());
            preparedStatement.setDouble(12, vendor.getLongitude());

            rowInserted = preparedStatement.executeUpdate() > 0;
        }

        return rowInserted;
    }
}