package DAO;

import Config.DB_TukangNow;
import Model.AdminHomeData;
import Model.AdminHomeVendor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class HomeAdminDAO {

    protected Connection getConnection() throws SQLException {
        Connection connection = DB_TukangNow.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public AdminHomeData getHomeAdminData(int adminId) throws SQLException {
        AdminHomeData data = new AdminHomeData();

        data.setAdminProfile(getAdminProfile(adminId));
        data.setNewApps(getCount("SELECT COUNT(*) FROM vendor WHERE status = 'pending'"));
        data.setActiveVendors(getCount("SELECT COUNT(*) FROM vendor WHERE status = 'active'"));
        data.setNewReports(0);
        data.setVendors(getActiveVendors());

        return data;
    }

    private String getAdminProfile(int adminId) throws SQLException {
        String sql = "SELECT profile_path FROM admin WHERE id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, adminId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return normalizeProfilePath(resultSet.getString("profile_path"));
                }
            }
        }

        return "";
    }

    private int getCount(String sql) throws SQLException {
        int count = 0;

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            if (resultSet.next()) {
                count = resultSet.getInt(1);
            }
        }

        return count;
    }

    private ArrayList<AdminHomeVendor> getActiveVendors() throws SQLException {
        ArrayList<AdminHomeVendor> vendors = new ArrayList<>();

        String sql = "SELECT name, nophone, doc_path, profile_path, status FROM vendor WHERE status = 'active'";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                AdminHomeVendor vendor = new AdminHomeVendor();

                vendor.setName(resultSet.getString("name"));
                vendor.setPhone(resultSet.getString("nophone"));
                vendor.setDocUrl(resultSet.getString("doc_path"));
                vendor.setProfilePath(normalizeProfilePath(resultSet.getString("profile_path")));
                vendor.setStatus(resultSet.getString("status"));
                vendor.setRating("0.0");
                vendor.setWorkDone("0");

                vendors.add(vendor);
            }
        }

        return vendors;
    }

    private String normalizeProfilePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }

        String cleanPath = path.trim().replace("\\", "/");

        if (cleanPath.startsWith("http://") || cleanPath.startsWith("https://")) {
            return cleanPath;
        }

        String[] parts = cleanPath.split("/");
        String fileName = parts[parts.length - 1];

        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }

        return "profiles/" + fileName.trim();
    }
}