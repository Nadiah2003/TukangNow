package DAO;

import Config.ConnectionManager;
import Model.ProfileCustomerData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProfileCustomerDAO {

    protected Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public ProfileCustomerData getProfile(int customerId) throws SQLException {
        String sql = "SELECT name, email, nophone, address, postcode, city, state, country, profile_path " +
                "FROM customer WHERE id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    ProfileCustomerData data = new ProfileCustomerData();

                    data.setStatus("success");
                    data.setName(resultSet.getString("name"));
                    data.setEmail(resultSet.getString("email"));
                    data.setNophone(resultSet.getString("nophone"));
                    data.setAddress(resultSet.getString("address"));
                    data.setPostcode(resultSet.getString("postcode"));
                    data.setCity(resultSet.getString("city"));
                    data.setState(normalizeState(resultSet.getString("state")));
                    data.setCountry(resultSet.getString("country"));
                    data.setProfile_path(safe(resultSet.getString("profile_path")));

                    return data;
                }
            }
        }

        return null;
    }

    public boolean updateProfile(int customerId, String name, String email, String phone, String address, String postcode, String city, String state, String country) throws SQLException {
        String sql = "UPDATE customer SET name = ?, email = ?, nophone = ?, address = ?, postcode = ?, city = ?, state = ?, country = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(name));
            preparedStatement.setString(2, safe(email));
            preparedStatement.setString(3, safe(phone));
            preparedStatement.setString(4, safe(address));
            preparedStatement.setString(5, safe(postcode));
            preparedStatement.setString(6, safe(city));
            preparedStatement.setString(7, normalizeState(state));
            preparedStatement.setString(8, safe(country));
            preparedStatement.setInt(9, customerId);

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean updateProfileImage(int customerId, String fileName) throws SQLException {
        String sql = "UPDATE customer SET profile_path = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(fileName));
            preparedStatement.setInt(2, customerId);

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean isOldPasswordCorrect(int customerId, String oldPassword) throws SQLException {
        String sql = "SELECT password FROM customer WHERE id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String currentPassword = resultSet.getString("password");

                    if (oldPassword == null || currentPassword == null) {
                        return false;
                    }

                    return oldPassword.equals(currentPassword);
                }
            }
        }

        return false;
    }

    public boolean changePassword(int customerId, String newPassword) throws SQLException {
        String sql = "UPDATE customer SET password = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(newPassword));
            preparedStatement.setInt(2, customerId);

            return preparedStatement.executeUpdate() > 0;
        }
    }

    private String normalizeState(String state) {
        String clean = safe(state);

        if (clean.equalsIgnoreCase("kl") || clean.equalsIgnoreCase("kuala lumpur") || clean.equals("W.P. Kuala Lumpur")) {
            return "Wilayah Persekutuan Kuala Lumpur";
        }

        return clean;
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}