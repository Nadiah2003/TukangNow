package DAO;

import Config.ConnectionManager;
import Model.ChatInfo;
import Model.ChatMessage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ChatDAO {

    private Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public ChatInfo getChatInfo(int sessionUserId, String sessionRole, String requestedView, int bookingId) throws SQLException {
        String sql = "SELECT " +
                "b.id AS booking_id, " +
                "b.customer_id, " +
                "IFNULL(c.name, 'Customer') AS customer_name, " +
                "IFNULL(s.vendor_id, 0) AS vendor_id, " +
                "IFNULL(v.name, 'Vendor') AS vendor_name " +
                "FROM booking b " +
                "LEFT JOIN customer c ON b.customer_id = c.id " +
                "LEFT JOIN service s ON b.service_id = s.id " +
                "LEFT JOIN vendor v ON s.vendor_id = v.id " +
                "WHERE b.id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, bookingId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int customerId = resultSet.getInt("customer_id");
                    int vendorId = resultSet.getInt("vendor_id");
                    String customerName = safe(resultSet.getString("customer_name"));
                    String vendorName = safe(resultSet.getString("vendor_name"));
                    String viewerRole = resolveViewerRole(sessionUserId, sessionRole, requestedView, customerId, vendorId);

                    if (viewerRole.isEmpty()) {
                        throw new SQLException("Cannot determine chat role. Please reopen chat from tracking page.");
                    }

                    ChatInfo info = new ChatInfo();
                    info.setStatus("success");
                    info.setBooking_id(bookingId);
                    info.setViewer_role(viewerRole);

                    if ("vendor".equals(viewerRole)) {
                        info.setPartner_name(customerName);
                    } else {
                        info.setPartner_name(vendorName);
                    }

                    return info;
                }
            }
        }

        return null;
    }

    public ArrayList<ChatMessage> getMessages(int sessionUserId, String viewerRole, int bookingId) throws SQLException {
        ArrayList<ChatMessage> messages = new ArrayList<>();

        String sql = "SELECT " +
                "id, booking_id, sender_id, sender_type, message, edited_message, is_edited, is_deleted, " +
                "DATE_FORMAT(time_sent, '%h:%i %p') AS time_sent " +
                "FROM chat_messages " +
                "WHERE booking_id = ? " +
                "ORDER BY id ASC";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, bookingId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    ChatMessage message = new ChatMessage();

                    int senderId = resultSet.getInt("sender_id");
                    String senderType = safe(resultSet.getString("sender_type"));
                    int isDeleted = resultSet.getInt("is_deleted");

                    message.setId(resultSet.getInt("id"));
                    message.setBooking_id(resultSet.getInt("booking_id"));
                    message.setSender_id(senderId);
                    message.setSender_type(senderType);
                    message.setMessage(safe(resultSet.getString("message")));
                    message.setEdited_message(safe(resultSet.getString("edited_message")));
                    message.setIs_edited(resultSet.getInt("is_edited"));
                    message.setIs_deleted(isDeleted);
                    message.setTime_sent(safe(resultSet.getString("time_sent")));
                    message.setCan_action(isDeleted == 0 && senderId == sessionUserId && senderType.equalsIgnoreCase(viewerRole));

                    messages.add(message);
                }
            }
        }

        return messages;
    }

    public boolean sendMessage(int bookingId, int senderId, String senderType, String message) throws SQLException {
        String sql = "INSERT INTO chat_messages (booking_id, sender_id, sender_type, message) VALUES (?, ?, ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, senderId);
            preparedStatement.setString(3, safe(senderType));
            preparedStatement.setString(4, safe(message));

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean updateMessage(int bookingId, int msgId, int senderId, String senderType, String message) throws SQLException {
        String sql = "UPDATE chat_messages " +
                "SET edited_message = ?, is_edited = 1 " +
                "WHERE id = ? AND booking_id = ? AND sender_id = ? AND sender_type = ? AND is_deleted = 0";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(message));
            preparedStatement.setInt(2, msgId);
            preparedStatement.setInt(3, bookingId);
            preparedStatement.setInt(4, senderId);
            preparedStatement.setString(5, safe(senderType));

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean deleteMessage(int bookingId, int msgId, int senderId, String senderType) throws SQLException {
        String sql = "UPDATE chat_messages " +
                "SET is_deleted = 1 " +
                "WHERE id = ? AND booking_id = ? AND sender_id = ? AND sender_type = ? AND is_deleted = 0";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, msgId);
            preparedStatement.setInt(2, bookingId);
            preparedStatement.setInt(3, senderId);
            preparedStatement.setString(4, safe(senderType));

            return preparedStatement.executeUpdate() > 0;
        }
    }

    private String resolveViewerRole(int sessionUserId, String sessionRole, String requestedView, int customerId, int vendorId) {
        String view = safe(requestedView).toLowerCase();
        String role = safe(sessionRole).toLowerCase();

        if ("vendor".equals(view) && sessionUserId == vendorId) {
            return "vendor";
        }

        if ("customer".equals(view) && sessionUserId == customerId) {
            return "customer";
        }

        if ("vendor".equals(role) && sessionUserId == vendorId) {
            return "vendor";
        }

        if ("customer".equals(role) && sessionUserId == customerId) {
            return "customer";
        }

        boolean matchCustomer = sessionUserId == customerId;
        boolean matchVendor = sessionUserId == vendorId;

        if (matchVendor && !matchCustomer) {
            return "vendor";
        }

        if (matchCustomer && !matchVendor) {
            return "customer";
        }

        if (matchCustomer && matchVendor) {
            return "";
        }

        return "";
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}