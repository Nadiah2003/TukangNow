package DAO;

import Config.ConnectionManager;
import Model.WalletData;
import Model.WalletTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WalletDAO {

    protected Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public WalletData getWalletData(int customerId) throws SQLException {
        WalletData data = new WalletData();

        try (Connection connection = getConnection()) {
            createWalletIfNotExists(connection, customerId);
            data.setStatus("success");
            data.setMessage("Wallet data loaded.");
            data.setBalance(getWalletBalance(connection, customerId));
            data.setTransactions(getWalletTransactions(connection, customerId));
        }

        return data;
    }

    public void processTopupSuccess(int customerId, double amount, String referenceId) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                createWalletIfNotExists(connection, customerId);

                if (!walletTransactionExists(connection, customerId, referenceId)) {
                    updateWalletBalance(connection, customerId, amount);
                    insertWalletTransaction(connection, customerId, amount, "topup", referenceId);
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

    public void transferToBank(int customerId, double amount, String bankName, String accountNo) throws SQLException {
        if (amount < 10) {
            throw new SQLException("Minimum transfer is RM 10.00");
        }

        if (bankName == null || bankName.trim().isEmpty()) {
            throw new SQLException("Bank name is required.");
        }

        if (accountNo == null || accountNo.trim().isEmpty()) {
            throw new SQLException("Account number is required.");
        }

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                createWalletIfNotExists(connection, customerId);

                double currentBalance = getWalletBalanceForUpdate(connection, customerId);

                if (amount > currentBalance) {
                    throw new SQLException("Insufficient wallet balance.");
                }

                updateWalletBalance(connection, customerId, -amount);
                String referenceId = "WD-" + System.currentTimeMillis();
                insertWalletTransaction(connection, customerId, amount, "transfer", referenceId);

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void createWalletIfNotExists(Connection connection, int customerId) throws SQLException {
        String sql = "INSERT IGNORE INTO customer_wallet (customer_id, balance, updated_at) VALUES (?, 0.00, NOW())";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);
            preparedStatement.executeUpdate();
        }
    }

    private double getWalletBalance(Connection connection, int customerId) throws SQLException {
        String sql = "SELECT balance FROM customer_wallet WHERE customer_id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("balance");
                }
            }
        }

        return 0.00;
    }

    private double getWalletBalanceForUpdate(Connection connection, int customerId) throws SQLException {
        String sql = "SELECT balance FROM customer_wallet WHERE customer_id = ? LIMIT 1 FOR UPDATE";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("balance");
                }
            }
        }

        return 0.00;
    }

    private List<WalletTransaction> getWalletTransactions(Connection connection, int customerId) throws SQLException {
        List<WalletTransaction> transactions = new ArrayList<>();

        String sql = "SELECT amount, type, reference_id, created_at " +
                "FROM wallet_transactions " +
                "WHERE customer_id = ? " +
                "ORDER BY created_at DESC " +
                "LIMIT 10";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    WalletTransaction transaction = new WalletTransaction();
                    String type = safe(resultSet.getString("type"));

                    transaction.setAmount(resultSet.getDouble("amount"));
                    transaction.setType(type);
                    transaction.setReference_id(resultSet.getString("reference_id"));
                    transaction.setCreated_at(resultSet.getString("created_at"));
                    transaction.setDate(resultSet.getString("created_at"));
                    transaction.setDescription(getTransactionDescription(type));

                    transactions.add(transaction);
                }
            }
        }

        return transactions;
    }

    private void updateWalletBalance(Connection connection, int customerId, double amount) throws SQLException {
        String sql = "UPDATE customer_wallet SET balance = balance + ?, updated_at = NOW() WHERE customer_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDouble(1, amount);
            preparedStatement.setInt(2, customerId);

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Failed to update wallet balance.");
            }
        }
    }

    private void insertWalletTransaction(Connection connection, int customerId, double amount, String type, String referenceId) throws SQLException {
        String sql = "INSERT INTO wallet_transactions (customer_id, amount, type, reference_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);
            preparedStatement.setDouble(2, amount);
            preparedStatement.setString(3, safe(type));
            preparedStatement.setString(4, safe(referenceId));
            preparedStatement.executeUpdate();
        }
    }

    private boolean walletTransactionExists(Connection connection, int customerId, String referenceId) throws SQLException {
        String sql = "SELECT id FROM wallet_transactions WHERE customer_id = ? AND reference_id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);
            preparedStatement.setString(2, safe(referenceId));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String getTransactionDescription(String type) {
        String cleanType = safe(type).toLowerCase();

        if ("topup".equals(cleanType)) {
            return "Wallet Top Up";
        }

        if ("transfer".equals(cleanType)) {
            return "Withdraw to Bank";
        }

        if ("payment".equals(cleanType)) {
            return "Service Payment";
        }

        if ("refund".equals(cleanType)) {
            return "Booking Refund";
        }

        if ("earnings".equals(cleanType)) {
            return "Wallet Earnings";
        }

        return "Wallet Transaction";
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}