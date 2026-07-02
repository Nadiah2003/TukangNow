package DAO;

import Config.ConnectionManager;
import Model.AddServiceInfo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AddServiceDAO {

    protected Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public AddServiceInfo getServiceInfo(int vendorId) throws SQLException {
        AddServiceInfo info = null;

        String sql = "SELECT servicename, subservice FROM service WHERE vendor_id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String existingSubServices = resultSet.getString("subservice");

                    if (existingSubServices == null || existingSubServices.trim().equalsIgnoreCase("null")) {
                        existingSubServices = "";
                    }

                    info = new AddServiceInfo();
                    info.setMainCategory(resultSet.getString("servicename"));
                    info.setExistingSubServices(existingSubServices);
                }
            }
        }

        return info;
    }

    public boolean updateSubServices(int vendorId, String subservices) throws SQLException {
        boolean success = false;

        String sql = "UPDATE service SET subservice = ? WHERE vendor_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, subservices);
            preparedStatement.setInt(2, vendorId);

            success = preparedStatement.executeUpdate() > 0;
        }

        return success;
    }
}