package DAO;

import Config.ConnectionManager;
import Model.Event;
import Model.Vendor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HomeCustomerDAO {

    public HomeCustomerDAO() {
    }

    protected Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public String[] getCustomerProfileAndState(int userId) throws SQLException {
        String[] data = new String[]{"image/profile.png", "0", "", "0.0", "0.0"};

        String sql = "SELECT profile_path, rewards_points, state, latitude, longitude FROM customer WHERE id = ? LIMIT 1";

        try (Connection conn = getConnection();
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

        createWalletIfNotExists(userId);

        String sql = "SELECT balance FROM customer_wallet WHERE customer_id = ? LIMIT 1";

        try (Connection conn = getConnection();
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

    private void createWalletIfNotExists(int customerId) {
        try (Connection conn = getConnection()) {
            String checkSql = "SELECT id FROM customer_wallet WHERE customer_id = ? LIMIT 1";

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, customerId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        return;
                    }
                }
            }

            String insertSql = "INSERT INTO customer_wallet (customer_id, balance, updated_at) VALUES (?, 0.00, NOW())";

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, customerId);
                insertStmt.executeUpdate();
            }
        } catch (Exception e) {
        }
    }

    public void updateExpiredEvents() {
        String sql = "UPDATE events SET status = 'inactive' WHERE end_date < CURDATE() AND LOWER(TRIM(status)) = 'active'";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.executeUpdate();
        } catch (Exception e) {
        }
    }

    public List<Event> getActiveEvents(int userId) throws SQLException {
        updateExpiredEvents();

        List<Event> events = new ArrayList<>();

        String sql = "SELECT e.id, e.title, e.description, e.image_path, e.discount_code, e.discount_percentage, e.start_date, e.end_date, "
                + "(SELECT COUNT(*) FROM customer_vouchers cv "
                + "WHERE cv.voucher_id = e.id "
                + "AND cv.customer_id = ? "
                + "AND LOWER(TRIM(cv.status)) IN ('active', 'unused')) AS is_redeemed "
                + "FROM events e "
                + "WHERE LOWER(TRIM(e.status)) = 'active' "
                + "AND CURDATE() BETWEEN e.start_date AND e.end_date "
                + "ORDER BY e.id DESC";

        try (Connection conn = getConnection();
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

        if (rangeKm <= 0) {
            rangeKm = 50;
        }

        if (rangeKm > 50) {
            rangeKm = 50;
        }

        String sql = "SELECT DISTINCT "
                + "v.id, "
                + "v.name, "
                + "v.profile_path, "
                + "v.doc_path, "
                + "(6371 * ACOS(LEAST(1, GREATEST(-1, "
                + "COS(RADIANS(?)) * COS(RADIANS(v.latitude)) * COS(RADIANS(v.longitude) - RADIANS(?)) + "
                + "SIN(RADIANS(?)) * SIN(RADIANS(v.latitude)) "
                + ")))) AS distance_km "
                + "FROM vendor v "
                + "JOIN service s ON v.id = s.vendor_id "
                + "WHERE LOWER(TRIM(v.status)) = 'active' "
                + "AND IFNULL(v.is_first_login, 0) = 0 "
                + "AND LOWER(TRIM(s.servicename)) = LOWER(TRIM(?)) "
                + "AND v.latitude IS NOT NULL "
                + "AND v.longitude IS NOT NULL "
                + "HAVING distance_km <= ? "
                + "ORDER BY distance_km " + orderDirection + ", v.name ASC "
                + "LIMIT 10";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, customerLatitude);
            stmt.setDouble(2, customerLongitude);
            stmt.setDouble(3, customerLatitude);
            stmt.setString(4, safe(category));
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

    public int getCustomerNotificationCount(int customerId) {
        int total = 0;

        total += getBookingStatusNotificationCount(customerId);
        total += getSecondPaymentNotificationCount(customerId);
        total += getReportStatusNotificationCount(customerId);
        total += getWalletNotificationCount(customerId);
        total += getVendorChatNotificationCount(customerId);
        total += getAdminChatNotificationCount(customerId);

        return total;
    }

    public List<Map<String, Object>> getCustomerNotifications(int customerId) {
        List<Map<String, Object>> notifications = new ArrayList<>();

        int bookingCount = getBookingStatusNotificationCount(customerId);
        int secondPaymentCount = getSecondPaymentNotificationCount(customerId);
        int reportCount = getReportStatusNotificationCount(customerId);
        int walletCount = getWalletNotificationCount(customerId);
        int vendorChatCount = getVendorChatNotificationCount(customerId);
        int adminChatCount = getAdminChatNotificationCount(customerId);

        notifications.add(buildNotification(
                "booking_status",
                "📦",
                "Booking Status",
                buildBookingStatusMessage(customerId, bookingCount),
                bookingCount,
                getLatestBookingStatusDate(customerId),
                "myorder.html"
        ));

        notifications.add(buildNotification(
                "second_payment",
                "💰",
                "Second Payment Required",
                buildSecondPaymentMessage(customerId, secondPaymentCount),
                secondPaymentCount,
                getLatestSecondPaymentDate(customerId),
                "myorder.html"
        ));

        notifications.add(buildNotification(
                "report_status",
                "⚠️",
                "Report Status",
                buildReportStatusMessage(customerId, reportCount),
                reportCount,
                getLatestReportDate(customerId),
                "myorder.html"
        ));

        notifications.add(buildNotification(
                "wallet",
                "💳",
                "E-Wallet Transaction",
                buildWalletMessage(customerId, walletCount),
                walletCount,
                getLatestWalletDate(customerId),
                "wallet.html"
        ));

        notifications.add(buildNotification(
                "vendor_chat",
                "💬",
                "Vendor Chat",
                vendorChatCount + " recent message(s) from vendor for your booking.",
                vendorChatCount,
                getLatestVendorChatDate(customerId),
                "myorder.html"
        ));

        notifications.add(buildNotification(
                "admin_chat",
                "🛎️",
                "Admin Chat",
                adminChatCount + " recent admin support message(s).",
                adminChatCount,
                getLatestAdminChatDate(customerId),
                "myorder.html"
        ));

        return notifications;
    }

    private Map<String, Object> buildNotification(String type, String icon, String title, String message, int count, String date, String link) {
        Map<String, Object> notification = new LinkedHashMap<>();

        notification.put("id", type + "_" + count + "_" + safe(date).replace(" ", "_"));
        notification.put("type", type);
        notification.put("icon", icon);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("count", count);
        notification.put("date", safe(date));
        notification.put("link", link);

        return notification;
    }

    private int getBookingStatusNotificationCount(int customerId) {
        String sql = "SELECT COUNT(id) AS total_count "
                + "FROM booking "
                + "WHERE customer_id = ? "
                + "AND LOWER(TRIM(status)) IN ('pending', 'emergency', 'accepted', 'on the way', 'arrived', 'started', 'second payment', 'completed', 'rated', 'reject', 'rejected', 'cancelled', 'payment failed', 'report') "
                + "AND (bookingdate >= DATE_SUB(NOW(), INTERVAL 14 DAY) "
                + "OR LOWER(TRIM(status)) IN ('pending', 'emergency', 'accepted', 'on the way', 'arrived', 'started', 'second payment', 'report'))";

        return getSafeCount(sql, customerId);
    }

    private int getSecondPaymentNotificationCount(int customerId) {
        String sql = "SELECT COUNT(DISTINCT b.id) AS total_count "
                + "FROM booking b "
                + "LEFT JOIN payment p ON p.booking_id = b.id AND p.customer_id = b.customer_id "
                + "WHERE b.customer_id = ? "
                + "AND (LOWER(TRIM(b.status)) = 'second payment' "
                + "OR (LOWER(TRIM(IFNULL(p.payment_type, ''))) = 'second_payment' "
                + "AND LOWER(TRIM(IFNULL(p.payment_status, ''))) IN ('pending', 'unpaid', 'created')))";

        return getSafeCount(sql, customerId);
    }

    private int getReportStatusNotificationCount(int customerId) {
        String sql = "SELECT COUNT(DISTINCT id) AS total_count "
                + "FROM reports "
                + "WHERE ((LOWER(TRIM(reporter_type)) = 'customer' AND reporter_id = ?) "
                + "OR (LOWER(TRIM(reported_type)) = 'customer' AND reported_id = ?)) "
                + "AND (LOWER(TRIM(IFNULL(status, ''))) IN ('submitted', 'investigating') "
                + "OR (created_at IS NOT NULL AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)))";

        return getSafeCount(sql, customerId, customerId);
    }

    private int getWalletNotificationCount(int customerId) {
        String sql = "SELECT COUNT(id) AS total_count "
                + "FROM wallet_transactions "
                + "WHERE customer_id = ?";

        return getSafeCount(sql, customerId);
    }

    private int getVendorChatNotificationCount(int customerId) {
        String sql = "SELECT COUNT(cm.id) AS total_count "
                + "FROM chat_messages cm "
                + "JOIN booking b ON cm.booking_id = b.id "
                + "WHERE b.customer_id = ? "
                + "AND LOWER(TRIM(cm.sender_type)) = 'vendor' "
                + "AND COALESCE(cm.is_deleted, 0) = 0";

        return getSafeCount(sql, customerId);
    }

    private int getAdminChatNotificationCount(int customerId) {
        String sql = "SELECT COUNT(ac.id) AS total_count "
                + "FROM admin_chats ac "
                + "JOIN booking b ON ac.booking_id = b.id "
                + "WHERE b.customer_id = ? "
                + "AND LOWER(TRIM(ac.sender)) = 'admin'";

        return getSafeCount(sql, customerId);
    }

    private String buildBookingStatusMessage(int customerId, int count) {
        if (count <= 0) {
            return "No booking status update right now.";
        }

        String sql = "SELECT id, status "
                + "FROM booking "
                + "WHERE customer_id = ? "
                + "AND LOWER(TRIM(status)) IN ('pending', 'emergency', 'accepted', 'on the way', 'arrived', 'started', 'second payment', 'completed', 'rated', 'reject', 'rejected', 'cancelled', 'payment failed', 'report') "
                + "ORDER BY bookingdate DESC, id DESC "
                + "LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return count + " booking update(s). Latest: Booking #" + rs.getInt("id") + " is " + safe(rs.getString("status")) + ".";
                }
            }
        } catch (Exception e) {
        }

        return count + " booking status update(s).";
    }

    private String buildSecondPaymentMessage(int customerId, int count) {
        if (count <= 0) {
            return "No second payment request right now.";
        }

        String sql = "SELECT b.id, COALESCE(p.final_amount, b.totalbalance, b.totalamount, 0.00) AS amount_due "
                + "FROM booking b "
                + "LEFT JOIN payment p ON p.booking_id = b.id AND p.customer_id = b.customer_id "
                + "WHERE b.customer_id = ? "
                + "AND (LOWER(TRIM(b.status)) = 'second payment' "
                + "OR (LOWER(TRIM(IFNULL(p.payment_type, ''))) = 'second_payment' "
                + "AND LOWER(TRIM(IFNULL(p.payment_status, ''))) IN ('pending', 'unpaid', 'created'))) "
                + "ORDER BY b.bookingdate DESC, b.id DESC "
                + "LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return "Booking #" + rs.getInt("id") + " has a second payment request. Amount due: RM " + String.format("%.2f", rs.getDouble("amount_due")) + ".";
                }
            }
        } catch (Exception e) {
        }

        return count + " second payment request(s) need your action.";
    }

    private String buildReportStatusMessage(int customerId, int count) {
        if (count <= 0) {
            return "No report status update right now.";
        }

        String sql = "SELECT id, status, IFNULL(action_taken, '') AS action_taken "
                + "FROM reports "
                + "WHERE ((LOWER(TRIM(reporter_type)) = 'customer' AND reporter_id = ?) "
                + "OR (LOWER(TRIM(reported_type)) = 'customer' AND reported_id = ?)) "
                + "ORDER BY id DESC "
                + "LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);
            stmt.setInt(2, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String actionTaken = safe(rs.getString("action_taken"));
                    String actionText = actionTaken.isEmpty() ? "" : " Action: " + actionTaken + ".";
                    return count + " report update(s). Latest: Report #" + rs.getInt("id") + " is " + safe(rs.getString("status")) + "." + actionText;
                }
            }
        } catch (Exception e) {
        }

        return count + " report status update(s).";
    }

    private String buildWalletMessage(int customerId, int count) {
        if (count <= 0) {
            return "No e-wallet transaction update right now.";
        }

        String sql = "SELECT type, amount "
                + "FROM wallet_transactions "
                + "WHERE customer_id = ? "
                + "ORDER BY id DESC "
                + "LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return count + " e-wallet transaction(s). Latest: " + safe(rs.getString("type")) + " RM " + String.format("%.2f", rs.getDouble("amount")) + ".";
                }
            }
        } catch (Exception e) {
        }

        return count + " e-wallet transaction update(s).";
    }

    private String getLatestBookingStatusDate(int customerId) {
        String sql = "SELECT IFNULL(DATE_FORMAT(bookingdate, '%Y-%m-%d %H:%i'), '') AS latest_date "
                + "FROM booking "
                + "WHERE customer_id = ? "
                + "ORDER BY bookingdate DESC, id DESC "
                + "LIMIT 1";

        return getSafeLatestDate(sql, customerId);
    }

    private String getLatestSecondPaymentDate(int customerId) {
        String sql = "SELECT IFNULL(DATE_FORMAT(b.bookingdate, '%Y-%m-%d %H:%i'), '') AS latest_date "
                + "FROM booking b "
                + "LEFT JOIN payment p ON p.booking_id = b.id AND p.customer_id = b.customer_id "
                + "WHERE b.customer_id = ? "
                + "AND (LOWER(TRIM(b.status)) = 'second payment' "
                + "OR (LOWER(TRIM(IFNULL(p.payment_type, ''))) = 'second_payment' "
                + "AND LOWER(TRIM(IFNULL(p.payment_status, ''))) IN ('pending', 'unpaid', 'created'))) "
                + "ORDER BY b.bookingdate DESC, b.id DESC "
                + "LIMIT 1";

        return getSafeLatestDate(sql, customerId);
    }

    private String getLatestReportDate(int customerId) {
        String sql = "SELECT '' AS latest_date "
                + "FROM reports "
                + "WHERE ((LOWER(TRIM(reporter_type)) = 'customer' AND reporter_id = ?) "
                + "OR (LOWER(TRIM(reported_type)) = 'customer' AND reported_id = ?)) "
                + "ORDER BY id DESC "
                + "LIMIT 1";

        return getSafeLatestDate(sql, customerId, customerId);
    }

    private String getLatestWalletDate(int customerId) {
        String sql = "SELECT '' AS latest_date "
                + "FROM wallet_transactions "
                + "WHERE customer_id = ? "
                + "ORDER BY id DESC "
                + "LIMIT 1";

        return getSafeLatestDate(sql, customerId);
    }

    private String getLatestVendorChatDate(int customerId) {
        String sql = "SELECT IFNULL(DATE_FORMAT(cm.time_sent, '%Y-%m-%d %H:%i'), '') AS latest_date "
                + "FROM chat_messages cm "
                + "JOIN booking b ON cm.booking_id = b.id "
                + "WHERE b.customer_id = ? "
                + "AND LOWER(TRIM(cm.sender_type)) = 'vendor' "
                + "AND COALESCE(cm.is_deleted, 0) = 0 "
                + "ORDER BY cm.time_sent DESC, cm.id DESC "
                + "LIMIT 1";

        return getSafeLatestDate(sql, customerId);
    }

    private String getLatestAdminChatDate(int customerId) {
        String sql = "SELECT '' AS latest_date "
                + "FROM admin_chats ac "
                + "JOIN booking b ON ac.booking_id = b.id "
                + "WHERE b.customer_id = ? "
                + "AND LOWER(TRIM(ac.sender)) = 'admin' "
                + "ORDER BY ac.id DESC "
                + "LIMIT 1";

        return getSafeLatestDate(sql, customerId);
    }

    private int getSafeCount(String sql, int... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setInt(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_count");
                }
            }
        } catch (Exception e) {
            return 0;
        }

        return 0;
    }

    private String getSafeLatestDate(String sql, int... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setInt(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return safe(rs.getString("latest_date"));
                }
            }
        } catch (Exception e) {
            return "";
        }

        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}