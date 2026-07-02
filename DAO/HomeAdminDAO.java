package DAO;

import Config.ConnectionManager;
import Model.AdminHomeData;
import Model.AdminHomeVendor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HomeAdminDAO {

    protected Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public AdminHomeData getHomeAdminData(int adminId) throws SQLException {
        AdminHomeData data = new AdminHomeData();

        data.setAdminProfile(getAdminProfile(adminId));
        data.setNewApps(getCount("SELECT COUNT(*) FROM vendor WHERE LOWER(TRIM(status)) = 'pending'"));
        data.setActiveVendors(getCount("SELECT COUNT(*) FROM vendor WHERE LOWER(TRIM(status)) = 'active'"));
        data.setNewReports(getSafeCount("SELECT COUNT(*) FROM reports WHERE LOWER(TRIM(status)) IN ('submitted', 'investigating')"));
        data.setVendors(getActiveVendors());

        return data;
    }

    public int getAdminLevel(int adminId) throws SQLException {
        String sql = "SELECT admin_level FROM admin WHERE id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, adminId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("admin_level");
                }
            }
        }

        return 0;
    }

    public List<Map<String, Object>> getNotifications(int adminId) throws SQLException {
        List<Map<String, Object>> notifications = new ArrayList<>();
        int adminLevel = getAdminLevel(adminId);

        int newApplicationCount = getSafeCount("SELECT COUNT(*) FROM vendor WHERE LOWER(TRIM(status)) = 'pending'");
        int newReportCount = getSafeCount("SELECT COUNT(*) FROM reports WHERE LOWER(TRIM(status)) IN ('submitted', 'investigating')");
        int newAdminCount = getSafeCountWithOneInt("SELECT COUNT(*) FROM admin WHERE id <> ?", adminId);
        int newPaymentCount = getSafeCount("SELECT COUNT(*) FROM payment WHERE (LOWER(TRIM(payment_status)) IN ('paid', 'success', 'completed') OR payment_paid > 0)");
        int suspendActiveCount = getSafeCount(
                "SELECT "
                + "(SELECT COUNT(*) FROM customer WHERE LOWER(TRIM(status)) = 'suspended' OR (suspendstartdate IS NOT NULL AND suspendenddate IS NOT NULL AND CURDATE() BETWEEN DATE(suspendstartdate) AND DATE(suspendenddate))) + "
                + "(SELECT COUNT(*) FROM vendor WHERE LOWER(TRIM(status)) = 'suspended' OR (suspendstartdate IS NOT NULL AND suspendenddate IS NOT NULL AND CURDATE() BETWEEN DATE(suspendstartdate) AND DATE(suspendenddate)))"
        );
        int suspendEndCount = getSafeCount(
                "SELECT "
                + "(SELECT COUNT(*) FROM customer WHERE LOWER(TRIM(status)) = 'suspended' AND suspendenddate IS NOT NULL AND DATE(suspendenddate) <= CURDATE()) + "
                + "(SELECT COUNT(*) FROM vendor WHERE LOWER(TRIM(status)) = 'suspended' AND suspendenddate IS NOT NULL AND DATE(suspendenddate) <= CURDATE())"
        );

        notifications.add(buildNotification("new_application", "📝", "New Application", newApplicationCount + " vendor application(s) waiting for review.", newApplicationCount, "newapplication.html"));
        notifications.add(buildNotification("new_report", "⚠️", "New Report", newReportCount + " report(s) need admin review.", newReportCount, "report.html"));
        notifications.add(buildNotification("new_admin", "👤", "Admin Account", newAdminCount + " other admin account(s) registered in the system.", newAdminCount, "profileadmin.html"));
        notifications.add(buildNotification("new_payment", "💳", "New Payment", newPaymentCount + " successful payment(s) received.", newPaymentCount, "homeadmin.html"));
        notifications.add(buildNotification("suspend_active", "⏳", "Suspension Active", suspendActiveCount + " customer/vendor account(s) currently suspended.", suspendActiveCount, "report.html"));
        notifications.add(buildNotification("suspend_end", "✅", "Suspension Ended", suspendEndCount + " suspended account(s) reached suspend end date.", suspendEndCount, "report.html"));

        if (adminLevel == 3) {
            int newReceiptCount = getSafeCount("SELECT COUNT(*) FROM booking_material_receipts");
            notifications.add(buildNotification("new_resit", "🧾", "New Receipt", newReceiptCount + " material receipt(s) uploaded for Estimate Admin.", newReceiptCount, "profileadmin.html"));
        }

        return notifications;
    }

    private Map<String, Object> buildNotification(String type, String icon, String title, String message, int count, String link) {
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("type", type);
        notification.put("icon", icon);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("count", count);
        notification.put("link", link);
        return notification;
    }

    private String getAdminProfile(int adminId) throws SQLException {
        String sql = "SELECT profile_path FROM admin WHERE id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, adminId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return normalizeFileName(resultSet.getString("profile_path"));
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

    private int getSafeCount(String sql) {
        try {
            return getCount(sql);
        } catch (Exception ex) {
            return 0;
        }
    }

    private int getSafeCountWithOneInt(String sql, int value) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, value);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (Exception ex) {
            return 0;
        }

        return 0;
    }

    private ArrayList<AdminHomeVendor> getActiveVendors() throws SQLException {
        ArrayList<AdminHomeVendor> vendors = new ArrayList<>();

        String sql = "SELECT "
                + "v.id, "
                + "v.name, "
                + "v.nophone, "
                + "v.doc_path, "
                + "v.profile_path, "
                + "v.status, "
                + "COALESCE(AVG(r.rating_val), 0) AS avg_rating, "
                + "COUNT(DISTINCT CASE WHEN LOWER(TRIM(b.status)) IN ('completed', 'rated') THEN b.id END) AS total_jobs "
                + "FROM vendor v "
                + "LEFT JOIN service s ON s.vendor_id = v.id "
                + "LEFT JOIN booking b ON b.service_id = s.id "
                + "LEFT JOIN rating r ON r.booking_id = b.id "
                + "WHERE LOWER(TRIM(v.status)) = 'active' "
                + "GROUP BY v.id, v.name, v.nophone, v.doc_path, v.profile_path, v.status "
                + "ORDER BY v.id DESC";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                AdminHomeVendor vendor = new AdminHomeVendor();

                vendor.setName(resultSet.getString("name"));
                vendor.setPhone(resultSet.getString("nophone"));
                vendor.setDocUrl(resultSet.getString("doc_path"));
                vendor.setProfilePath(normalizeFileName(resultSet.getString("profile_path")));
                vendor.setStatus(resultSet.getString("status"));
                vendor.setRating(String.format("%.1f", resultSet.getDouble("avg_rating")));
                vendor.setWorkDone(String.valueOf(resultSet.getInt("total_jobs")));

                vendors.add(vendor);
            }
        }

        return vendors;
    }

    private String normalizeFileName(String path) {
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

        return fileName.trim();
    }
}