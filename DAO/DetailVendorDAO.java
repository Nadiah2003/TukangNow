package DAO;

import Config.ConnectionManager;
import Model.DetailVendor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DetailVendorDAO {

    protected Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public DetailVendor getVendorApplication(int vendorId) throws SQLException {
        DetailVendor vendor = null;

        String sql = "SELECT id, name, nophone, email, profile_path, doc_path FROM vendor WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    vendor = new DetailVendor();
                    vendor.setId(resultSet.getInt("id"));
                    vendor.setName(resultSet.getString("name"));
                    vendor.setPhone(resultSet.getString("nophone"));
                    vendor.setEmail(resultSet.getString("email"));
                    vendor.setProfilePath(resultSet.getString("profile_path"));
                    vendor.setLicenseUrl(resultSet.getString("doc_path"));
                }
            }
        }

        return vendor;
    }

    public boolean approveVendor(int vendorId, String expiry, List<String> services, int adminId) throws SQLException {
        boolean success = false;

        String updateVendorSql = "UPDATE vendor SET status = 'active', expireddate = ?, review_admin_id = ?, review_action = 'approved', review_date = NOW() WHERE id = ?";
        String deleteServiceSql = "DELETE FROM service WHERE vendor_id = ?";
        String insertServiceSql = "INSERT INTO service (vendor_id, servicename, subservice, startprice, avail_date, avail_time) VALUES (?, ?, '', 0.00, NULL, NULL)";

        Connection connection = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement updateVendorStmt = connection.prepareStatement(updateVendorSql)) {
                updateVendorStmt.setString(1, expiry);
                updateVendorStmt.setInt(2, adminId);
                updateVendorStmt.setInt(3, vendorId);

                int vendorUpdated = updateVendorStmt.executeUpdate();

                if (vendorUpdated <= 0) {
                    connection.rollback();
                    return false;
                }
            }

            try (PreparedStatement deleteServiceStmt = connection.prepareStatement(deleteServiceSql)) {
                deleteServiceStmt.setInt(1, vendorId);
                deleteServiceStmt.executeUpdate();
            }

            try (PreparedStatement insertServiceStmt = connection.prepareStatement(insertServiceSql)) {
                for (String service : services) {
                    String serviceName = service == null ? "" : service.trim();

                    if (!serviceName.isEmpty()) {
                        insertServiceStmt.setInt(1, vendorId);
                        insertServiceStmt.setString(2, serviceName);
                        insertServiceStmt.addBatch();
                    }
                }

                insertServiceStmt.executeBatch();
            }

            connection.commit();
            success = true;

        } catch (SQLException e) {
            if (connection != null) {
                connection.rollback();
            }

            throw e;

        } finally {
            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        }

        return success;
    }

    public boolean updateVendorStatus(int vendorId, String status, int adminId) throws SQLException {
        boolean success = false;

        String action = status.equalsIgnoreCase("rejected") ? "rejected" : status;

        String sql = "UPDATE vendor SET status = ?, review_admin_id = ?, review_action = ?, review_date = NOW() WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, status);
            preparedStatement.setInt(2, adminId);
            preparedStatement.setString(3, action);
            preparedStatement.setInt(4, vendorId);

            success = preparedStatement.executeUpdate() > 0;
        }

        return success;
    }
}