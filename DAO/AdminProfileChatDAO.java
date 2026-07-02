package DAO;

import Config.ConnectionManager;
import Model.AdminProfileChatConversation;
import Model.AdminProfileChatMessage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class AdminProfileChatDAO {

    private Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null.");
        }

        return connection;
    }

    public int getUnreadCountForAdmin(int adminId) throws SQLException {
        String sql =
                "SELECT COUNT(*) AS total_unread " +
                "FROM admin_chats ac " +
                "LEFT JOIN admin_chat_assignment aca ON aca.booking_id = ac.booking_id " +
                "WHERE LOWER(TRIM(ac.sender)) = 'customer' " +
                "AND IFNULL(ac.admin_read, 0) = 0 " +
                "AND (aca.admin_id IS NULL OR aca.admin_id = ?)";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, adminId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total_unread");
                }
            }
        }

        return 0;
    }

    public ArrayList<AdminProfileChatConversation> getConversations(int adminId) throws SQLException {
        ArrayList<AdminProfileChatConversation> conversations = new ArrayList<>();

        String sql =
                "SELECT " +
                "b.id AS booking_id, " +
                "IFNULL(c.name, 'Customer') AS customer_name, " +
                "IFNULL(s.servicename, 'Service') AS service_name, " +
                "IFNULL(b.status, '') AS booking_status, " +
                "IFNULL(latest.sender, '') AS last_sender, " +
                "IFNULL(latest.message, '') AS last_message, " +
                "IFNULL(DATE_FORMAT(latest.created_at, '%Y-%m-%d %H:%i'), '') AS last_time, " +
                "IFNULL(SUM(CASE WHEN LOWER(TRIM(ac.sender)) = 'customer' AND IFNULL(ac.admin_read, 0) = 0 THEN 1 ELSE 0 END), 0) AS unread_count " +
                "FROM booking b " +
                "INNER JOIN admin_chats ac ON ac.booking_id = b.id " +
                "LEFT JOIN customer c ON b.customer_id = c.id " +
                "LEFT JOIN service s ON b.service_id = s.id " +
                "LEFT JOIN admin_chat_assignment aca ON aca.booking_id = b.id " +
                "LEFT JOIN admin_chats latest ON latest.id = ( " +
                "SELECT ac2.id FROM admin_chats ac2 WHERE ac2.booking_id = b.id ORDER BY ac2.id DESC LIMIT 1 " +
                ") " +
                "WHERE aca.admin_id IS NULL OR aca.admin_id = ? " +
                "GROUP BY b.id, c.name, s.servicename, b.status, latest.sender, latest.message, latest.created_at " +
                "ORDER BY latest.created_at DESC, b.id DESC";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, adminId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    AdminProfileChatConversation conversation = new AdminProfileChatConversation();

                    conversation.setBookingId(resultSet.getInt("booking_id"));
                    conversation.setCustomerName(safe(resultSet.getString("customer_name")));
                    conversation.setServiceName(safe(resultSet.getString("service_name")));
                    conversation.setStatus(safe(resultSet.getString("booking_status")));
                    conversation.setLastSender(safe(resultSet.getString("last_sender")));
                    conversation.setLastMessage(safe(resultSet.getString("last_message")));
                    conversation.setLastTime(safe(resultSet.getString("last_time")));
                    conversation.setUnreadCount(resultSet.getInt("unread_count"));

                    conversations.add(conversation);
                }
            }
        }

        return conversations;
    }

    public ArrayList<AdminProfileChatMessage> getMessages(int adminId, int bookingId) throws SQLException {
        ArrayList<AdminProfileChatMessage> messages = new ArrayList<>();

        String sql =
                "SELECT " +
                "id, " +
                "booking_id, " +
                "IFNULL(sender, '') AS sender, " +
                "IFNULL(message, '') AS message, " +
                "IFNULL(DATE_FORMAT(created_at, '%Y-%m-%d %H:%i'), '') AS created_at " +
                "FROM admin_chats " +
                "WHERE booking_id = ? " +
                "ORDER BY id ASC";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                claimConversation(connection, adminId, bookingId);

                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setInt(1, bookingId);

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            AdminProfileChatMessage message = new AdminProfileChatMessage();

                            message.setId(resultSet.getInt("id"));
                            message.setBookingId(resultSet.getInt("booking_id"));
                            message.setSender(safe(resultSet.getString("sender")));
                            message.setMessage(safe(resultSet.getString("message")));
                            message.setCreatedAt(safe(resultSet.getString("created_at")));

                            messages.add(message);
                        }
                    }
                }

                markAsRead(connection, bookingId);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }

        return messages;
    }

    public void sendAdminMessage(int adminId, int bookingId, String message) throws SQLException {
        String cleanMessage = safe(message);

        if (cleanMessage.isEmpty()) {
            throw new SQLException("Message cannot be empty.");
        }

        String sql =
                "INSERT INTO admin_chats " +
                "(booking_id, sender, message, admin_read, created_at) " +
                "VALUES (?, 'admin', ?, 1, NOW())";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                claimConversation(connection, adminId, bookingId);

                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setInt(1, bookingId);
                    preparedStatement.setString(2, cleanMessage);
                    preparedStatement.executeUpdate();
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void claimConversation(Connection connection, int adminId, int bookingId) throws SQLException {
        String selectSql =
                "SELECT admin_id " +
                "FROM admin_chat_assignment " +
                "WHERE booking_id = ? " +
                "LIMIT 1 FOR UPDATE";

        String insertSql =
                "INSERT INTO admin_chat_assignment " +
                "(booking_id, admin_id, assigned_at) " +
                "VALUES (?, ?, NOW())";

        try (PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
            selectStatement.setInt(1, bookingId);

            try (ResultSet resultSet = selectStatement.executeQuery()) {
                if (resultSet.next()) {
                    int assignedAdminId = resultSet.getInt("admin_id");

                    if (assignedAdminId != adminId) {
                        throw new SQLException("This chat is already handled by another admin.");
                    }

                    return;
                }
            }
        }

        try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            insertStatement.setInt(1, bookingId);
            insertStatement.setInt(2, adminId);
            insertStatement.executeUpdate();
        }
    }

    private void markAsRead(Connection connection, int bookingId) throws SQLException {
        String sql =
                "UPDATE admin_chats " +
                "SET admin_read = 1 " +
                "WHERE booking_id = ? " +
                "AND LOWER(TRIM(sender)) = 'customer'";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.executeUpdate();
        }
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}