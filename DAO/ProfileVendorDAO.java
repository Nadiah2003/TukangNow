package DAO;

import Config.DB_TukangNow;
import Model.ProfileServiceInfo;
import Model.ProfileVendorData;
import Model.ProfileVendorInfo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

public class ProfileVendorDAO {

    protected Connection getConnection() throws SQLException {
        Connection connection = DB_TukangNow.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public ProfileVendorData getProfileData(int vendorId) throws SQLException {
        ProfileVendorData data = new ProfileVendorData();

        data.setStatus("success");
        data.setVendor(getVendorInfo(vendorId));
        data.setService(getServiceInfo(vendorId));
        loadStats(vendorId, data);

        return data;
    }

    private ProfileVendorInfo getVendorInfo(int vendorId) throws SQLException {
        ProfileVendorInfo vendor = null;

        String sql = "SELECT name, email, nophone, COALESCE(address, '') AS address, COALESCE(postcode, '') AS postcode, COALESCE(city, '') AS city, COALESCE(state, '') AS state, profile_path FROM vendor WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    vendor = new ProfileVendorInfo();

                    vendor.setName(resultSet.getString("name"));
                    vendor.setEmail(resultSet.getString("email"));
                    vendor.setNophone(resultSet.getString("nophone"));
                    vendor.setAddress(resultSet.getString("address"));
                    vendor.setPostcode(resultSet.getString("postcode"));
                    vendor.setCity(resultSet.getString("city"));
                    vendor.setState(resultSet.getString("state"));
                    vendor.setProfile_path(resultSet.getString("profile_path"));
                    vendor.buildFullDisplayAddress();
                }
            }
        }

        return vendor;
    }

    private ProfileServiceInfo getServiceInfo(int vendorId) throws SQLException {
        ProfileServiceInfo service = null;

        String sql = "SELECT servicename, COALESCE(subservice, '') AS subServices, startprice, avail_time, avail_date FROM service WHERE vendor_id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    service = new ProfileServiceInfo();

                    service.setServicename(resultSet.getString("servicename"));
                    service.setSubServices(resultSet.getString("subServices"));
                    service.setStartprice(resultSet.getDouble("startprice"));
                    service.setAvail_time(resultSet.getString("avail_time"));
                    service.setAvail_date(resultSet.getString("avail_date"));

                    String rawSubServices = service.getSubServices();

                    if (rawSubServices != null && !rawSubServices.trim().isEmpty()) {
                        service.setSubServicesList(Arrays.asList(rawSubServices.split("\\s*,\\s*")));
                    } else {
                        service.setSubServicesList(Collections.emptyList());
                    }
                }
            }
        }

        return service;
    }

    private void loadStats(int vendorId, ProfileVendorData data) throws SQLException {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM booking b JOIN service s ON b.service_id = s.id WHERE s.vendor_id = ? AND b.status = 'Completed') AS jobsCompleted, " +
                "COALESCE(AVG(r.rating_val), 0.0) AS avgRating, " +
                "COUNT(r.id) AS totalReviews " +
                "FROM vendor v " +
                "LEFT JOIN service s ON v.id = s.vendor_id " +
                "LEFT JOIN booking b ON s.id = b.service_id " +
                "LEFT JOIN rating r ON b.id = r.booking_id " +
                "WHERE v.id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);
            preparedStatement.setInt(2, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    data.setJobsCompleted(resultSet.getInt("jobsCompleted"));
                    data.setAvgRating(String.format("%.1f", resultSet.getDouble("avgRating")));
                    data.setTotalReviews(resultSet.getInt("totalReviews"));
                }
            }
        }
    }

    public boolean updateProfileInfo(int vendorId, String name, String email, String phone, String address, String postcode, String city, String state) throws SQLException {
        String sql = "UPDATE vendor SET name = ?, email = ?, nophone = ?, address = ?, postcode = ?, city = ?, state = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(name));
            preparedStatement.setString(2, safe(email));
            preparedStatement.setString(3, safe(phone));
            preparedStatement.setString(4, safe(address));
            preparedStatement.setString(5, safe(postcode));
            preparedStatement.setString(6, safe(city));
            preparedStatement.setString(7, safe(state));
            preparedStatement.setInt(8, vendorId);

            preparedStatement.executeUpdate();
            return true;
        }
    }

    public boolean updateService(int vendorId, double price, String subservices) throws SQLException {
        String sql = "UPDATE service SET startprice = ?, subservice = ? WHERE vendor_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setDouble(1, price);
            preparedStatement.setString(2, safe(subservices));
            preparedStatement.setInt(3, vendorId);

            preparedStatement.executeUpdate();
            return true;
        }
    }

    public boolean updateAvailability(int vendorId, String days, String time) throws SQLException {
        String sql = "UPDATE service SET avail_date = ?, avail_time = ? WHERE vendor_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(days));
            preparedStatement.setString(2, safe(time));
            preparedStatement.setInt(3, vendorId);

            preparedStatement.executeUpdate();
            return true;
        }
    }

    public String updatePassword(int vendorId, String oldPass, String newPass) throws SQLException {
        String currentPassword = null;

        String sqlCheck = "SELECT password FROM vendor WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sqlCheck)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    currentPassword = resultSet.getString("password");
                }
            }
        }

        if (currentPassword == null || !currentPassword.equals(oldPass)) {
            return "Old password incorrect.";
        }

        String sqlUpdate = "UPDATE vendor SET password = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sqlUpdate)) {

            preparedStatement.setString(1, newPass);
            preparedStatement.setInt(2, vendorId);

            preparedStatement.executeUpdate();
            return "success";
        }
    }

    public boolean updateProfileImage(int vendorId, String fileName) throws SQLException {
        String sql = "UPDATE vendor SET profile_path = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, fileName);
            preparedStatement.setInt(2, vendorId);

            preparedStatement.executeUpdate();
            return true;
        }
    }

    public void checkAndDisableFirstLogin(int vendorId) throws SQLException {
        String sql = "UPDATE vendor v " +
                "LEFT JOIN service s ON v.id = s.vendor_id " +
                "SET v.is_first_login = CASE " +
                "WHEN s.id IS NOT NULL " +
                "AND s.subservice IS NOT NULL " +
                "AND TRIM(s.subservice) <> '' " +
                "AND s.startprice IS NOT NULL " +
                "AND s.startprice > 0 " +
                "AND s.avail_date IS NOT NULL " +
                "AND TRIM(CAST(s.avail_date AS CHAR)) <> '' " +
                "AND s.avail_time IS NOT NULL " +
                "AND TRIM(s.avail_time) <> '' " +
                "THEN 0 ELSE 1 END " +
                "WHERE v.id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);
            preparedStatement.executeUpdate();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}