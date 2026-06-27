package DAO;

import Config.DB_TukangNow;
import Model.EmergencyBookingResult;
import Model.EmergencyCustomerData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EmergencyDAO {

    private static final double EMERGENCY_RADIUS_KM = 30.0;

    private Connection getConnection() throws SQLException {
        Connection connection = DB_TukangNow.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public EmergencyCustomerData getCustomerData(int customerId) throws SQLException {
        String sql = "SELECT id, name, email, nophone, address, postcode, city, state, latitude, longitude FROM customer WHERE id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    EmergencyCustomerData data = new EmergencyCustomerData();

                    String address = safe(resultSet.getString("address"));
                    String postcode = safe(resultSet.getString("postcode"));
                    String city = safe(resultSet.getString("city"));
                    String state = safe(resultSet.getString("state"));
                    double customerLatitude = resultSet.getDouble("latitude");
                    double customerLongitude = resultSet.getDouble("longitude");
                    String fullAddress = buildFullAddress(address, postcode, city, state);

                    data.setStatus("success");
                    data.setId(resultSet.getInt("id"));
                    data.setCustName(safe(resultSet.getString("name")));
                    data.setCustEmail(safe(resultSet.getString("email")));
                    data.setCustPhone(safe(resultSet.getString("nophone")));
                    data.setCustAddress(fullAddress);

                    if (!isValidCoordinate(customerLatitude, customerLongitude)) {
                        data.addServiceIds("Plumbing", "");
                        data.addServiceIds("Electrical", "");
                        data.addServiceIds("Aircond", "");
                        return data;
                    }

                    data.addServiceIds("Plumbing", findEmergencyServiceIdsCsv(connection, customerLatitude, customerLongitude, "Plumbing"));
                    data.addServiceIds("Electrical", findEmergencyServiceIdsCsv(connection, customerLatitude, customerLongitude, "Electrical"));
                    data.addServiceIds("Aircond", findEmergencyServiceIdsCsv(connection, customerLatitude, customerLongitude, "Aircond"));

                    return data;
                }
            }
        }

        return null;
    }

    public EmergencyBookingResult createEmergencyBooking(int customerId, String category, String problem) throws SQLException {
        EmergencyBookingResult result = new EmergencyBookingResult();

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                CustomerCoordinate customerCoordinate = getCustomerCoordinate(connection, customerId);

                if (customerCoordinate == null || !isValidCoordinate(customerCoordinate.latitude, customerCoordinate.longitude)) {
                    throw new SQLException("Customer location coordinate not found. Please update your profile location first.");
                }

                String serviceIds = findEmergencyServiceIdsCsv(connection, customerCoordinate.latitude, customerCoordinate.longitude, category);

                if (serviceIds.isEmpty()) {
                    throw new SQLException("Tiada vendor dalam radius 30km bagi servis '" + safe(category) + "' buat masa ini.");
                }

                int primaryServiceId = getFirstServiceId(serviceIds);

                if (primaryServiceId <= 0) {
                    throw new SQLException("Emergency service ID tidak sah.");
                }

                String sql = "INSERT INTO booking " +
                        "(customer_id, service_id, subservicebooked, problem, bookingdate, status, deposit, travelfee, totalamount, distancekm, evidencepath) " +
                        "VALUES (?, ?, 'Emergency', ?, NOW(), 'Emergency', 50.00, 0.00, 50.00, 0.00, '')";

                try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setInt(1, customerId);
                    preparedStatement.setInt(2, primaryServiceId);
                    preparedStatement.setString(3, safeDefault(problem, "Emergency SOS"));

                    int affectedRows = preparedStatement.executeUpdate();

                    if (affectedRows == 0) {
                        throw new SQLException("Gagal memasukkan data tempahan kecemasan.");
                    }

                    try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int bookingId = generatedKeys.getInt(1);
                            insertEmergencyBookingServices(connection, bookingId, serviceIds);

                            connection.commit();

                            result.setStatus("success");
                            result.setMessage("Emergency booking created.");
                            result.setBookingId(bookingId);

                            return result;
                        }
                    }
                }

                throw new SQLException("Gagal mendapatkan booking ID.");
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private CustomerCoordinate getCustomerCoordinate(Connection connection, int customerId) throws SQLException {
        String sql = "SELECT latitude, longitude FROM customer WHERE id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    CustomerCoordinate coordinate = new CustomerCoordinate();
                    coordinate.latitude = resultSet.getDouble("latitude");
                    coordinate.longitude = resultSet.getDouble("longitude");
                    return coordinate;
                }
            }
        }

        return null;
    }

    private String findEmergencyServiceIdsCsv(Connection connection, double customerLatitude, double customerLongitude, String category) throws SQLException {
        String keywordOne = getKeywordOne(category);
        String keywordTwo = getKeywordTwo(category);

        String sql = "SELECT s.id, v.latitude, v.longitude " +
                "FROM service s " +
                "INNER JOIN vendor v ON s.vendor_id = v.id " +
                "WHERE LOWER(TRIM(v.status)) = 'active' " +
                "AND (LOWER(TRIM(s.servicename)) = LOWER(TRIM(?)) " +
                "OR LOWER(TRIM(s.servicename)) LIKE ? " +
                "OR LOWER(TRIM(s.servicename)) LIKE ?) " +
                "ORDER BY s.id ASC";

        List<String> matchedServiceIds = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, safe(category));
            preparedStatement.setString(2, "%" + keywordOne + "%");
            preparedStatement.setString(3, "%" + keywordTwo + "%");

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    double vendorLatitude = resultSet.getDouble("latitude");
                    double vendorLongitude = resultSet.getDouble("longitude");

                    if (!isValidCoordinate(vendorLatitude, vendorLongitude)) {
                        continue;
                    }

                    double distance = calculateDistanceKm(customerLatitude, customerLongitude, vendorLatitude, vendorLongitude);

                    if (distance <= EMERGENCY_RADIUS_KM) {
                        matchedServiceIds.add(String.valueOf(resultSet.getInt("id")));
                    }
                }
            }
        }

        return String.join(",", matchedServiceIds);
    }

    private void insertEmergencyBookingServices(Connection connection, int bookingId, String serviceIds) throws SQLException {
        String sql = "INSERT IGNORE INTO emergency_booking_services " +
                "(booking_id, service_id, notification_status, notified_at) " +
                "VALUES (?, ?, 'pending', NOW())";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (String serviceIdText : serviceIds.split(",")) {
                int serviceId = parseInt(serviceIdText);

                if (serviceId <= 0) {
                    continue;
                }

                preparedStatement.setInt(1, bookingId);
                preparedStatement.setInt(2, serviceId);
                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();
        }
    }

    private int getFirstServiceId(String serviceIds) {
        if (serviceIds == null || serviceIds.trim().isEmpty()) {
            return 0;
        }

        String[] parts = serviceIds.split(",");

        if (parts.length == 0) {
            return 0;
        }

        return parseInt(parts[0]);
    }

    private String getKeywordOne(String category) {
        String clean = safe(category).toLowerCase();

        if (clean.contains("plumb")) {
            return "plumb";
        }

        if (clean.contains("elect")) {
            return "elect";
        }

        if (clean.contains("air")) {
            return "aircond";
        }

        return clean;
    }

    private String getKeywordTwo(String category) {
        String clean = safe(category).toLowerCase();

        if (clean.contains("plumb")) {
            return "pipe";
        }

        if (clean.contains("elect")) {
            return "wire";
        }

        if (clean.contains("air")) {
            return "air";
        }

        return clean;
    }

    private double calculateDistanceKm(double customerLatitude, double customerLongitude, double vendorLatitude, double vendorLongitude) {
        double earthRadiusKm = 6371.0;
        double latitudeDistance = Math.toRadians(vendorLatitude - customerLatitude);
        double longitudeDistance = Math.toRadians(vendorLongitude - customerLongitude);

        double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(Math.toRadians(customerLatitude)) * Math.cos(Math.toRadians(vendorLatitude))
                * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }

    private boolean isValidCoordinate(double latitude, double longitude) {
        if (latitude == 0.0 && longitude == 0.0) {
            return false;
        }

        return latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0;
    }

    private String buildFullAddress(String address, String postcode, String city, String state) {
        String firstPart = safe(address);
        String secondPart = (safe(postcode) + " " + safe(city)).trim();
        String thirdPart = safe(state);

        StringBuilder builder = new StringBuilder();

        if (!firstPart.isEmpty()) {
            builder.append(firstPart);
        }

        if (!secondPart.isEmpty()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }

            builder.append(secondPart);
        }

        if (!thirdPart.isEmpty()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }

            builder.append(thirdPart);
        }

        return builder.toString();
    }

    private int parseInt(String value) {
        try {
            if (value != null && !value.trim().isEmpty()) {
                return Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException e) {
            return 0;
        }

        return 0;
    }

    private String safeDefault(String value, String fallback) {
        String clean = safe(value);

        if (clean.isEmpty()) {
            return fallback;
        }

        return clean;
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private static class CustomerCoordinate {
        private double latitude;
        private double longitude;
    }
}