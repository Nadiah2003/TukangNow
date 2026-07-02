package DAO;

import Config.ConnectionManager;
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
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public EmergencyCustomerData getCustomerData(int customerId) throws SQLException {
        String sql = "SELECT id, name, email, nophone, address, postcode, city, state, latitude, longitude " +
                "FROM customer " +
                "WHERE id = ? " +
                "LIMIT 1";

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
                        data.addServiceIds("Plumber", "");
                        data.addServiceIds("Electrical", "");
                        data.addServiceIds("Electrician", "");
                        data.addServiceIds("Aircond", "");
                        data.addServiceIds("Air Conditioner", "");
                        return data;
                    }

                    String plumbingIds = findEmergencyServiceIdsCsv(connection, customerLatitude, customerLongitude, "Plumbing");
                    String electricalIds = findEmergencyServiceIdsCsv(connection, customerLatitude, customerLongitude, "Electrical");
                    String aircondIds = findEmergencyServiceIdsCsv(connection, customerLatitude, customerLongitude, "Aircond");

                    data.addServiceIds("Plumbing", plumbingIds);
                    data.addServiceIds("Plumber", plumbingIds);

                    data.addServiceIds("Electrical", electricalIds);
                    data.addServiceIds("Electrician", electricalIds);

                    data.addServiceIds("Aircond", aircondIds);
                    data.addServiceIds("Air Conditioner", aircondIds);

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

                            int insertedCount = insertEmergencyBookingServices(connection, bookingId, serviceIds);

                            if (insertedCount <= 0) {
                                throw new SQLException("Emergency booking created but failed to notify vendors.");
                            }

                            connection.commit();

                            result.setStatus("success");
                            result.setMessage("Emergency booking created and broadcasted to " + insertedCount + " vendor service(s).");
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
        String sql = "SELECT latitude, longitude " +
                "FROM customer " +
                "WHERE id = ? " +
                "LIMIT 1";

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
        List<Integer> matchedServiceIds = findEmergencyServiceIds(connection, customerLatitude, customerLongitude, category);
        List<String> serviceIdTexts = new ArrayList<>();

        for (Integer serviceId : matchedServiceIds) {
            if (serviceId != null && serviceId > 0) {
                serviceIdTexts.add(String.valueOf(serviceId));
            }
        }

        return String.join(",", serviceIdTexts);
    }

    private List<Integer> findEmergencyServiceIds(Connection connection, double customerLatitude, double customerLongitude, String category) throws SQLException {
        List<Integer> matchedServiceIds = new ArrayList<>();

        String cleanCategory = normalizeCategory(category);
        List<String> keywords = getCategoryKeywords(cleanCategory);

        if (keywords.isEmpty()) {
            keywords.add(cleanCategory.toLowerCase());
        }

        String sql = "SELECT DISTINCT " +
                "s.id AS service_id, " +
                "s.servicename, " +
                "s.subservice, " +
                "v.id AS vendor_id, " +
                "v.name AS vendor_name, " +
                "v.latitude, " +
                "v.longitude, " +
                "(6371 * ACOS(LEAST(1, GREATEST(-1, " +
                "COS(RADIANS(?)) * COS(RADIANS(v.latitude)) * COS(RADIANS(v.longitude) - RADIANS(?)) + " +
                "SIN(RADIANS(?)) * SIN(RADIANS(v.latitude)) " +
                ")))) AS distance_km " +
                "FROM service s " +
                "INNER JOIN vendor v ON s.vendor_id = v.id " +
                "WHERE LOWER(TRIM(v.status)) = 'active' " +
                "AND v.latitude IS NOT NULL " +
                "AND v.longitude IS NOT NULL " +
                "HAVING distance_km <= ? " +
                "ORDER BY distance_km ASC, s.id ASC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDouble(1, customerLatitude);
            preparedStatement.setDouble(2, customerLongitude);
            preparedStatement.setDouble(3, customerLatitude);
            preparedStatement.setDouble(4, EMERGENCY_RADIUS_KM);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int serviceId = resultSet.getInt("service_id");
                    double vendorLatitude = resultSet.getDouble("latitude");
                    double vendorLongitude = resultSet.getDouble("longitude");
                    String serviceName = safe(resultSet.getString("servicename"));
                    String subservice = safe(resultSet.getString("subservice"));

                    if (serviceId <= 0) {
                        continue;
                    }

                    if (!isValidCoordinate(vendorLatitude, vendorLongitude)) {
                        continue;
                    }

                    if (!isSameEmergencyCategory(cleanCategory, serviceName, subservice, keywords)) {
                        continue;
                    }

                    if (!matchedServiceIds.contains(serviceId)) {
                        matchedServiceIds.add(serviceId);
                    }
                }
            }
        }

        return matchedServiceIds;
    }

    private boolean isSameEmergencyCategory(String category, String serviceName, String subservice, List<String> keywords) {
        String cleanCategory = safe(category).toLowerCase();
        String cleanServiceName = safe(serviceName).toLowerCase();
        String cleanSubservice = safe(subservice).toLowerCase();

        if (!cleanCategory.isEmpty()) {
            if (cleanServiceName.equals(cleanCategory)) {
                return true;
            }

            if (cleanServiceName.contains(cleanCategory)) {
                return true;
            }

            if (cleanSubservice.equals(cleanCategory)) {
                return true;
            }

            if (cleanSubservice.contains(cleanCategory)) {
                return true;
            }
        }

        for (String keyword : keywords) {
            String cleanKeyword = safe(keyword).toLowerCase();

            if (cleanKeyword.isEmpty()) {
                continue;
            }

            if (cleanServiceName.contains(cleanKeyword)) {
                return true;
            }

            if (cleanSubservice.contains(cleanKeyword)) {
                return true;
            }
        }

        return false;
    }

    private int insertEmergencyBookingServices(Connection connection, int bookingId, String serviceIds) throws SQLException {
        List<Integer> cleanedServiceIds = parseEmergencyServiceIds(serviceIds);

        if (cleanedServiceIds.isEmpty()) {
            return 0;
        }

        String sql = "INSERT IGNORE INTO emergency_booking_services " +
                "(booking_id, service_id, notification_status, notified_at) " +
                "VALUES (?, ?, 'pending', NOW())";

        int insertedCount = 0;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (Integer serviceId : cleanedServiceIds) {
                if (serviceId == null || serviceId <= 0) {
                    continue;
                }

                preparedStatement.setInt(1, bookingId);
                preparedStatement.setInt(2, serviceId);
                preparedStatement.addBatch();
            }

            int[] results = preparedStatement.executeBatch();

            for (int batchResult : results) {
                if (batchResult >= 0 || batchResult == Statement.SUCCESS_NO_INFO) {
                    insertedCount++;
                }
            }
        }

        return insertedCount;
    }

    private List<Integer> parseEmergencyServiceIds(String serviceIds) {
        List<Integer> cleanedServiceIds = new ArrayList<>();

        String clean = safe(serviceIds);

        if (clean.isEmpty()) {
            return cleanedServiceIds;
        }

        String[] parts = clean.split(",");

        for (String part : parts) {
            int serviceId = parseInt(part);

            if (serviceId <= 0) {
                continue;
            }

            if (!cleanedServiceIds.contains(serviceId)) {
                cleanedServiceIds.add(serviceId);
            }
        }

        return cleanedServiceIds;
    }

    private int getFirstServiceId(String serviceIds) {
        List<Integer> cleanedServiceIds = parseEmergencyServiceIds(serviceIds);

        if (cleanedServiceIds.isEmpty()) {
            return 0;
        }

        return cleanedServiceIds.get(0);
    }

    private String normalizeCategory(String category) {
        String clean = safe(category).toLowerCase();

        if (clean.contains("plumb") || clean.contains("pipe") || clean.contains("paip")) {
            return "Plumbing";
        }

        if (clean.contains("elect") || clean.contains("wire") || clean.contains("wiring")) {
            return "Electrical";
        }

        if (clean.contains("air") || clean.contains("aircond") || clean.contains("air conditioner")) {
            return "Aircond";
        }

        return safe(category);
    }

    private List<String> getCategoryKeywords(String category) {
        List<String> keywords = new ArrayList<>();

        String clean = safe(category).toLowerCase();

        if (clean.contains("plumb")) {
            keywords.add("plumbing");
            keywords.add("plumber");
            keywords.add("plumb");
            keywords.add("pipe");
            keywords.add("paip");
            return keywords;
        }

        if (clean.contains("elect")) {
            keywords.add("electrical");
            keywords.add("electrician");
            keywords.add("electric");
            keywords.add("elect");
            keywords.add("wire");
            keywords.add("wiring");
            return keywords;
        }

        if (clean.contains("air")) {
            keywords.add("aircond");
            keywords.add("air conditioner");
            keywords.add("aircon");
            keywords.add("air conditioning");
            keywords.add("air");
            return keywords;
        }

        keywords.add(clean);

        return keywords;
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