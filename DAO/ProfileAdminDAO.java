package DAO;

import Config.ConnectionManager;
import Model.AdminProfile;
import Model.AdminUser;
import Model.EventData;
import Model.EstimateItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProfileAdminDAO {

    private Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public AdminProfile getAdminProfile(int adminId) throws SQLException {
        updateExpiredEvents();

        String sql = "SELECT id, name, email, admin_level, profile_path FROM admin WHERE id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, adminId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    AdminProfile admin = new AdminProfile();
                    admin.setId(resultSet.getInt("id"));
                    admin.setName(resultSet.getString("name"));
                    admin.setEmail(resultSet.getString("email"));
                    admin.setAdmin_level(resultSet.getInt("admin_level"));
                    admin.setProfile_path(resultSet.getString("profile_path"));
                    return admin;
                }
            }
        }

        return null;
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

        return -1;
    }

    public boolean updateProfile(int adminId, String name, String email) throws SQLException {
        String sql = "UPDATE admin SET name = ?, email = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(name));
            preparedStatement.setString(2, safe(email));
            preparedStatement.setInt(3, adminId);

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean updateProfileImage(int adminId, String profilePath) throws SQLException {
        String sql = "UPDATE admin SET profile_path = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(profilePath));
            preparedStatement.setInt(2, adminId);

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean isOldPasswordCorrect(int adminId, String oldPass) throws SQLException {
        String sql = "SELECT password FROM admin WHERE id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, adminId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String currentPassword = resultSet.getString("password");

                    if (oldPass == null || currentPassword == null) {
                        return false;
                    }

                    return oldPass.equals(currentPassword);
                }
            }
        }

        return false;
    }

    public boolean changePassword(int adminId, String newPass) throws SQLException {
        String sql = "UPDATE admin SET password = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(newPass));
            preparedStatement.setInt(2, adminId);

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public List<AdminUser> getOtherAdmins(int currentAdminId) throws SQLException {
        List<AdminUser> admins = new ArrayList<>();
        String sql = "SELECT id, name, email, admin_level FROM admin WHERE id <> ? ORDER BY CASE WHEN admin_level = 1 THEN 1 WHEN admin_level = 2 THEN 2 WHEN admin_level = 3 THEN 3 ELSE 4 END ASC, name ASC";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, currentAdminId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    AdminUser admin = new AdminUser();
                    admin.setId(resultSet.getInt("id"));
                    admin.setName(resultSet.getString("name"));
                    admin.setEmail(resultSet.getString("email"));
                    admin.setAdmin_level(resultSet.getInt("admin_level"));
                    admins.add(admin);
                }
            }
        }

        return admins;
    }

    public boolean updateAdminRole(int targetAdminId, int adminLevel) throws SQLException {
        String sql = "UPDATE admin SET admin_level = ?, role = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, adminLevel);
            preparedStatement.setString(2, getRoleName(adminLevel));
            preparedStatement.setInt(3, targetAdminId);

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean insertEvent(EventData eventData) throws SQLException {
        updateExpiredEvents();

        String sql = "INSERT INTO events (title, description, image_path, discount_code, discount_percentage, start_date, end_date, status, admin_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(eventData.getTitle()));
            preparedStatement.setString(2, safe(eventData.getDescription()));
            preparedStatement.setString(3, safe(eventData.getImage_path()));
            preparedStatement.setString(4, safe(eventData.getDiscount_code()));
            preparedStatement.setDouble(5, eventData.getDiscount_percentage());
            preparedStatement.setString(6, safe(eventData.getStart_date()));
            preparedStatement.setString(7, safe(eventData.getEnd_date()));
            preparedStatement.setString(8, safe(eventData.getStatus()));
            preparedStatement.setInt(9, eventData.getAdmin_id());

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public void updateExpiredEvents() throws SQLException {
        String sql = "UPDATE events SET status = 'inactive' WHERE end_date < CURDATE() AND status = 'active'";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.executeUpdate();
        }
    }

    public List<EstimateItem> getEstimateItems() throws SQLException {
        List<EstimateItem> items = new ArrayList<>();
        String sql = "SELECT id, service_keyword, item_name, item_price FROM estimate_item ORDER BY service_keyword ASC, item_name ASC";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                EstimateItem item = new EstimateItem();
                item.setId(resultSet.getInt("id"));
                item.setServiceKeyword(resultSet.getString("service_keyword"));
                item.setItemName(resultSet.getString("item_name"));
                item.setItemPrice(resultSet.getDouble("item_price"));
                items.add(item);
            }
        }

        return items;
    }

    public boolean insertEstimateItem(EstimateItem item) throws SQLException {
        String sql = "INSERT INTO estimate_item (service_keyword, item_name, item_price) VALUES (?, ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(item.getServiceKeyword()));
            preparedStatement.setString(2, safe(item.getItemName()));
            preparedStatement.setDouble(3, item.getItemPrice());

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean updateEstimateItem(EstimateItem item) throws SQLException {
        String sql = "UPDATE estimate_item SET service_keyword = ?, item_name = ?, item_price = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(item.getServiceKeyword()));
            preparedStatement.setString(2, safe(item.getItemName()));
            preparedStatement.setDouble(3, item.getItemPrice());
            preparedStatement.setInt(4, item.getId());

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean deleteEstimateItem(int estimateId) throws SQLException {
        String sql = "DELETE FROM estimate_item WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, estimateId);

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public List<Map<String, Object>> getBookingMaterialItems() throws SQLException {
        List<Map<String, Object>> materials = new ArrayList<>();

        String sql = "SELECT "
                + "i.id, "
                + "i.booking_id, "
                + "i.receipt_id, "
                + "COALESCE(NULLIF(r.receipt_label, ''), CONCAT('Receipt ', i.receipt_id)) AS receipt_label, "
                + "i.item_name, "
                + "i.quantity, "
                + "i.price, "
                + "COALESCE(NULLIF(i.receipt_path, ''), NULLIF(r.receipt_path, '')) AS receipt_path, "
                + "DATE_FORMAT(i.created_at, '%Y-%m-%d %H:%i') AS created_at "
                + "FROM booking_material_items i "
                + "LEFT JOIN booking_material_receipts r "
                + "ON i.receipt_id = r.id AND i.booking_id = r.booking_id "
                + "ORDER BY i.created_at DESC, i.booking_id DESC, i.id DESC";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", resultSet.getInt("id"));
                item.put("bookingId", resultSet.getInt("booking_id"));
                item.put("receiptId", resultSet.getInt("receipt_id"));
                item.put("receiptLabel", resultSet.getString("receipt_label"));
                item.put("itemName", resultSet.getString("item_name"));
                item.put("quantity", resultSet.getString("quantity"));
                item.put("price", resultSet.getDouble("price"));
                item.put("receiptPath", resultSet.getString("receipt_path") == null ? "" : resultSet.getString("receipt_path"));
                item.put("createdAt", resultSet.getString("created_at") == null ? "" : resultSet.getString("created_at"));
                materials.add(item);
            }
        }

        return materials;
    }

    private String getRoleName(int adminLevel) {
        if (adminLevel == 1) {
            return "Leader Admin";
        }

        if (adminLevel == 2) {
            return "Event Admin";
        }

        if (adminLevel == 3) {
            return "Estimate Admin";
        }

        return "Admin";
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}