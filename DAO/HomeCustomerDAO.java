package DAO;

import Config.DB_TukangNow;
import Model.Event;
import Model.Vendor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HomeCustomerDAO {

    public HomeCustomerDAO() {}

    public String[] getCustomerProfileAndState(int userId) throws SQLException {
        String[] data = new String[]{"image/profile.png", "0", "", "0.0", "0.0"};

        String sql = "SELECT profile_path, rewards_points, state, latitude, longitude FROM customer WHERE id = ?";

        try (Connection conn = DB_TukangNow.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String profilePath = rs.getString("profile_path");

                    if (profilePath != null && !profilePath.trim().isEmpty()) {
                        data[0] = profilePath.trim();
                    }

                    data[1] = String.valueOf(rs.getInt("rewards_points"));
                    data[2] = rs.getString("state") != null ? rs.getString("state").trim() : "";
                    data[3] = String.valueOf(rs.getDouble("latitude"));
                    data[4] = String.valueOf(rs.getDouble("longitude"));
                }
            }
        }

        return data;
    }

    public String getWalletBalance(int userId) throws SQLException {
        String balance = "0.00";

        String sql = "SELECT balance FROM customer_wallet WHERE customer_id = ?";

        try (Connection conn = DB_TukangNow.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getBigDecimal("balance") != null) {
                    balance = String.format("%.2f", rs.getBigDecimal("balance"));
                }
            }
        }

        return balance;
    }

    public void updateExpiredEvents() throws SQLException {
        String sql = "UPDATE events SET status = 'inactive' WHERE end_date < CURDATE() AND LOWER(TRIM(status)) = 'active'";

        try (Connection conn = DB_TukangNow.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.executeUpdate();
        }
    }

    public List<Event> getActiveEvents(int userId) throws SQLException {
        updateExpiredEvents();

        List<Event> events = new ArrayList<>();

        String sql = "SELECT e.id, e.title, e.description, e.image_path, e.discount_code, e.discount_percentage, e.start_date, e.end_date, " +
                "(SELECT COUNT(*) FROM customer_vouchers cv WHERE cv.voucher_id = e.id AND cv.customer_id = ? AND LOWER(TRIM(cv.status)) = 'active') AS is_redeemed " +
                "FROM events e " +
                "WHERE LOWER(TRIM(e.status)) = 'active' " +
                "AND CURDATE() BETWEEN e.start_date AND e.end_date " +
                "ORDER BY e.id DESC";

        try (Connection conn = DB_TukangNow.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Event ev = new Event();

                    ev.setId(rs.getInt("id"));
                    ev.setTitle(rs.getString("title"));
                    ev.setDescription(rs.getString("description"));
                    ev.setImg(resolveEventImagePath(rs.getString("image_path")));
                    ev.setDiscountCode(rs.getString("discount_code"));
                    ev.setDiscountPercentage(rs.getInt("discount_percentage"));
                    ev.setStartDate(rs.getString("start_date"));
                    ev.setEndDate(rs.getString("end_date"));
                    ev.setIsRedeemed(rs.getInt("is_redeemed") > 0);

                    events.add(ev);
                }
            }
        }

        return events;
    }

    private String resolveEventImagePath(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return "image/profile.png";
        }

        String cleanPath = imagePath.trim().replace("\\", "/");

        if (cleanPath.startsWith("http://") || cleanPath.startsWith("https://")) {
            return cleanPath;
        }

        if (cleanPath.startsWith("image/")) {
            return cleanPath;
        }

        String fileName = cleanPath;

        if (cleanPath.contains("/")) {
            fileName = cleanPath.substring(cleanPath.lastIndexOf("/") + 1);
        }

        if (fileName.trim().isEmpty()) {
            return "image/profile.png";
        }

        return "events/" + fileName;
    }

    public List<Vendor> getVendorsByCategoryAndRange(String category, double customerLatitude, double customerLongitude, int rangeKm, String sortDirection) throws SQLException {
        List<Vendor> vendors = new ArrayList<>();

        String orderDirection = "ASC";

        if ("desc".equalsIgnoreCase(sortDirection)) {
            orderDirection = "DESC";
        }

        if (rangeKm > 50) {
            rangeKm = 50;
        }

        String sql = "SELECT DISTINCT " +
                "v.id, " +
                "v.name, " +
                "v.profile_path, " +
                "v.doc_path, " +
                "(6371 * ACOS(LEAST(1, GREATEST(-1, " +
                "COS(RADIANS(?)) * COS(RADIANS(v.latitude)) * COS(RADIANS(v.longitude) - RADIANS(?)) + " +
                "SIN(RADIANS(?)) * SIN(RADIANS(v.latitude)) " +
                ")))) AS distance_km " +
                "FROM vendor v " +
                "JOIN service s ON v.id = s.vendor_id " +
                "WHERE v.status = 'active' " +
                "AND v.is_first_login = 0 " +
                "AND s.servicename = ? " +
                "AND v.latitude IS NOT NULL " +
                "AND v.longitude IS NOT NULL " +
                "HAVING distance_km <= ? " +
                "ORDER BY distance_km " + orderDirection + ", v.name ASC " +
                "LIMIT 10";

        try (Connection conn = DB_TukangNow.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, customerLatitude);
            stmt.setDouble(2, customerLongitude);
            stmt.setDouble(3, customerLatitude);
            stmt.setString(4, category);
            stmt.setInt(5, rangeKm);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Vendor vendor = new Vendor(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("profile_path"),
                            rs.getString("doc_path")
                    );

                    vendor.setDistanceKm(rs.getDouble("distance_km"));
                    vendors.add(vendor);
                }
            }
        }

        return vendors;
    }
}