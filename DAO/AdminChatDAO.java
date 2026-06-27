package DAO;

import Config.DB_TukangNow;
import Model.AdminChat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class AdminChatDAO {

    private Connection getConnection() throws SQLException {
        Connection connection = DB_TukangNow.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null.");
        }

        return connection;
    }

    public ArrayList<AdminChat> getChats(int customerId, int bookingId) throws SQLException {
        ArrayList<AdminChat> chatList = new ArrayList<>();

        String sql =
                "SELECT " +
                "IFNULL(ac.sender, '') AS sender, " +
                "IFNULL(ac.message, '') AS message, " +
                "IFNULL(DATE_FORMAT(ac.created_at, '%Y-%m-%d %H:%i'), '') AS created_at " +
                "FROM admin_chats ac " +
                "JOIN booking b ON ac.booking_id = b.id " +
                "WHERE ac.booking_id = ? " +
                "AND b.customer_id = ? " +
                "ORDER BY ac.id ASC";

        try (Connection connection = getConnection()) {
            if (!isCustomerBooking(connection, customerId, bookingId)) {
                throw new SQLException("Invalid order.");
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, bookingId);
                preparedStatement.setInt(2, customerId);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        AdminChat chat = new AdminChat();
                        chat.setSender(safe(resultSet.getString("sender")));
                        chat.setMessage(safe(resultSet.getString("message")));
                        chat.setCreatedAt(safe(resultSet.getString("created_at")));
                        chatList.add(chat);
                    }
                }
            }
        }

        return chatList;
    }

    public void sendCustomerMessage(int customerId, int bookingId, String message) throws SQLException {
        String cleanMessage = safe(message);

        if (cleanMessage.isEmpty()) {
            throw new SQLException("Message cannot be empty.");
        }

        String sql =
                "INSERT INTO admin_chats " +
                "(booking_id, sender, message, created_at) " +
                "VALUES (?, 'customer', ?, NOW())";

        try (Connection connection = getConnection()) {
            if (!isCustomerBooking(connection, customerId, bookingId)) {
                throw new SQLException("Invalid order.");
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, bookingId);
                preparedStatement.setString(2, cleanMessage);
                preparedStatement.executeUpdate();
            }
        }
    }

    private boolean isCustomerBooking(Connection connection, int customerId, int bookingId) throws SQLException {
        String sql =
                "SELECT id " +
                "FROM booking " +
                "WHERE id = ? " +
                "AND customer_id = ? " +
                "LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}