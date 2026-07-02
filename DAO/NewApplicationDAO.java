package DAO;

import Config.ConnectionManager;
import Model.NewApplicationVendor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class NewApplicationDAO {

    protected Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public ArrayList<NewApplicationVendor> getPendingVendors() throws SQLException {
        ArrayList<NewApplicationVendor> pendingVendors = new ArrayList<>();

        String sql = "SELECT id, name, nophone, profile_path, doc_path FROM vendor WHERE status = 'pending'";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                NewApplicationVendor vendor = new NewApplicationVendor();

                vendor.setId(resultSet.getInt("id"));
                vendor.setName(resultSet.getString("name"));
                vendor.setNophone(resultSet.getString("nophone"));
                vendor.setProfilePath(resultSet.getString("profile_path"));
                vendor.setLicenseUrl(resultSet.getString("doc_path"));

                pendingVendors.add(vendor);
            }
        }

        return pendingVendors;
    }
}