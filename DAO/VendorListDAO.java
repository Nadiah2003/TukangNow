package DAO;

import Config.ConnectionManager;
import Model.VendorListItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VendorListDAO {

    private Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public List<VendorListItem> getVendorList(int customerId, String type, double minRating, Double maxPrice, String sortBy, double radiusKm) throws SQLException {
        List<VendorListItem> vendors = new ArrayList<>();

        CustomerLocation customerLocation = getCustomerLocation(customerId);

        if (customerLocation == null || !isValidCoordinate(customerLocation.latitude, customerLocation.longitude)) {
            throw new SQLException("Customer location coordinate not found. Please update your profile location first.");
        }

        String normalizedType = normalizeServiceType(type);
        String keyword = getServiceKeyword(normalizedType);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("v.id, ");
        sql.append("v.name, ");
        sql.append("v.profile_path, ");
        sql.append("v.latitude, ");
        sql.append("v.longitude, ");
        sql.append("COALESCE(CAST(NULLIF(TRIM(CAST(s.startprice AS CHAR)), '') AS DECIMAL(10,2)), 0.00) AS depositPrice, ");
        sql.append("COALESCE(AVG(r.rating_val), 0) AS rating, ");
        sql.append("COUNT(DISTINCT r.id) AS reviewCount ");
        sql.append("FROM vendor v ");
        sql.append("INNER JOIN service s ON v.id = s.vendor_id ");
        sql.append("LEFT JOIN booking b ON s.id = b.service_id ");
        sql.append("LEFT JOIN rating r ON b.id = r.booking_id ");
        sql.append("WHERE LOWER(TRIM(v.status)) = 'active' ");
        sql.append("AND (LOWER(TRIM(s.servicename)) = LOWER(TRIM(?)) OR LOWER(TRIM(s.servicename)) LIKE ?) ");
        sql.append("AND s.subservice IS NOT NULL ");
        sql.append("AND TRIM(CAST(s.subservice AS CHAR)) <> '' ");
        sql.append("AND s.startprice IS NOT NULL ");
        sql.append("AND TRIM(CAST(s.startprice AS CHAR)) <> '' ");
        sql.append("AND CAST(NULLIF(TRIM(CAST(s.startprice AS CHAR)), '') AS DECIMAL(10,2)) > 0 ");
        sql.append("AND s.avail_time IS NOT NULL ");
        sql.append("AND TRIM(CAST(s.avail_time AS CHAR)) <> '' ");
        sql.append("AND s.avail_date IS NOT NULL ");
        sql.append("AND TRIM(CAST(s.avail_date AS CHAR)) <> '' ");
        sql.append("GROUP BY v.id, v.name, v.profile_path, v.latitude, v.longitude, s.startprice ");
        sql.append("HAVING rating >= ? ");

        if (maxPrice != null) {
            sql.append("AND depositPrice <= ? ");
        }

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {

            int index = 1;
            preparedStatement.setString(index++, normalizedType);
            preparedStatement.setString(index++, "%" + keyword.toLowerCase() + "%");
            preparedStatement.setDouble(index++, minRating);

            if (maxPrice != null) {
                preparedStatement.setDouble(index++, maxPrice);
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    double vendorLat = resultSet.getDouble("latitude");
                    double vendorLng = resultSet.getDouble("longitude");

                    if (!isValidCoordinate(vendorLat, vendorLng)) {
                        continue;
                    }

                    double distance = calculateDistanceKm(customerLocation.latitude, customerLocation.longitude, vendorLat, vendorLng);

                    if (distance > radiusKm || distance > 50.0) {
                        continue;
                    }

                    VendorListItem vendor = new VendorListItem();
                    vendor.setId(resultSet.getInt("id"));
                    vendor.setName(resultSet.getString("name"));
                    vendor.setProfile_path(resultSet.getString("profile_path"));
                    vendor.setDepositPrice(resultSet.getDouble("depositPrice"));
                    vendor.setRating(roundOneDecimal(resultSet.getDouble("rating")));
                    vendor.setReviewCount(resultSet.getInt("reviewCount"));
                    vendor.setDistance(roundOneDecimal(distance));

                    vendors.add(vendor);
                }
            }
        }

        sortVendors(vendors, sortBy);
        return vendors;
    }

    private CustomerLocation getCustomerLocation(int customerId) throws SQLException {
        String sql = "SELECT latitude, longitude FROM customer WHERE id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    CustomerLocation location = new CustomerLocation();
                    location.latitude = resultSet.getDouble("latitude");
                    location.longitude = resultSet.getDouble("longitude");
                    return location;
                }
            }
        }

        return null;
    }

    private void sortVendors(List<VendorListItem> vendors, String sortBy) {
        String cleanSort = sortBy == null ? "distance" : sortBy.trim().toLowerCase();

        if ("rating".equals(cleanSort)) {
            Collections.sort(vendors, new Comparator<VendorListItem>() {
                @Override
                public int compare(VendorListItem a, VendorListItem b) {
                    return Double.compare(b.getRating(), a.getRating());
                }
            });
            return;
        }

        if ("price_asc".equals(cleanSort)) {
            Collections.sort(vendors, new Comparator<VendorListItem>() {
                @Override
                public int compare(VendorListItem a, VendorListItem b) {
                    return Double.compare(a.getDepositPrice(), b.getDepositPrice());
                }
            });
            return;
        }

        if ("price_desc".equals(cleanSort)) {
            Collections.sort(vendors, new Comparator<VendorListItem>() {
                @Override
                public int compare(VendorListItem a, VendorListItem b) {
                    return Double.compare(b.getDepositPrice(), a.getDepositPrice());
                }
            });
            return;
        }

        Collections.sort(vendors, new Comparator<VendorListItem>() {
            @Override
            public int compare(VendorListItem a, VendorListItem b) {
                return Double.compare(a.getDistance(), b.getDistance());
            }
        });
    }

    private double calculateDistanceKm(double customerLat, double customerLng, double vendorLat, double vendorLng) {
        double earthRadiusKm = 6371.0;
        double latDistance = Math.toRadians(vendorLat - customerLat);
        double lngDistance = Math.toRadians(vendorLng - customerLng);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(customerLat)) * Math.cos(Math.toRadians(vendorLat))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }

    private boolean isValidCoordinate(double latitude, double longitude) {
        if (latitude == 0.0 && longitude == 0.0) {
            return false;
        }

        return latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0;
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String normalizeServiceType(String type) {
        String clean = type == null ? "" : type.trim().toLowerCase();

        if (clean.contains("elect")) {
            return "Electrical";
        }

        if (clean.contains("plumb")) {
            return "Plumber";
        }

        if (clean.contains("lawn")) {
            return "Lawn";
        }

        if (clean.isEmpty()) {
            return "";
        }

        return clean.substring(0, 1).toUpperCase() + clean.substring(1);
    }

    private String getServiceKeyword(String type) {
        String clean = type == null ? "" : type.trim().toLowerCase();

        if (clean.contains("elect")) {
            return "elect";
        }

        if (clean.contains("plumb")) {
            return "plumb";
        }

        if (clean.contains("lawn")) {
            return "lawn";
        }

        return clean;
    }

    private static class CustomerLocation {
        private double latitude;
        private double longitude;
    }
}