package DAO;

import Config.DB_TukangNow;
import Model.VendorWallet;
import Model.VendorWalletTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class VendorWalletDAO {

    private Connection getConnection() throws SQLException {
        Connection connection = DB_TukangNow.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null.");
        }

        return connection;
    }

    public VendorWallet getWalletSummary(int vendorId) throws SQLException {
        syncPaidPaymentsToWallet(vendorId);
        rebuildWalletBalance(vendorId);

        VendorWallet wallet = new VendorWallet();
        wallet.setVendorId(vendorId);

        String walletSql =
                "SELECT IFNULL(balance, 0.00) AS balance " +
                "FROM vendor_wallet " +
                "WHERE vendor_id = ? " +
                "LIMIT 1";

        String summarySql =
                "SELECT " +
                "IFNULL(SUM(CASE WHEN UPPER(type) = 'CREDIT' THEN amount ELSE 0 END), 0.00) AS total_earned, " +
                "IFNULL(SUM(CASE WHEN UPPER(type) = 'WITHDRAW' THEN amount ELSE 0 END), 0.00) AS total_withdrawn " +
                "FROM vendor_wallet_transaction " +
                "WHERE vendor_id = ?";

        try (Connection connection = getConnection()) {
            ensureWalletRow(connection, vendorId);

            try (PreparedStatement walletStatement = connection.prepareStatement(walletSql)) {
                walletStatement.setInt(1, vendorId);

                try (ResultSet resultSet = walletStatement.executeQuery()) {
                    if (resultSet.next()) {
                        wallet.setBalance(resultSet.getDouble("balance"));
                    }
                }
            }

            try (PreparedStatement summaryStatement = connection.prepareStatement(summarySql)) {
                summaryStatement.setInt(1, vendorId);

                try (ResultSet resultSet = summaryStatement.executeQuery()) {
                    if (resultSet.next()) {
                        wallet.setTotalEarned(resultSet.getDouble("total_earned"));
                        wallet.setTotalWithdrawn(resultSet.getDouble("total_withdrawn"));
                    }
                }
            }
        }

        return wallet;
    }

    public ArrayList<VendorWalletTransaction> getWalletTransactions(int vendorId) throws SQLException {
        ArrayList<VendorWalletTransaction> transactionList = new ArrayList<>();

        String sql =
                "SELECT " +
                "id, " +
                "vendor_id, " +
                "IFNULL(booking_id, 0) AS booking_id, " +
                "IFNULL(type, '') AS type, " +
                "IFNULL(amount, 0.00) AS amount, " +
                "IFNULL(description, '') AS description, " +
                "IFNULL(DATE_FORMAT(created_at, '%Y-%m-%d %H:%i'), '') AS created_at " +
                "FROM vendor_wallet_transaction " +
                "WHERE vendor_id = ? " +
                "ORDER BY created_at DESC, id DESC " +
                "LIMIT 100";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    VendorWalletTransaction transaction = new VendorWalletTransaction();

                    transaction.setId(resultSet.getInt("id"));
                    transaction.setVendorId(resultSet.getInt("vendor_id"));
                    transaction.setBookingId(resultSet.getInt("booking_id"));
                    transaction.setType(safe(resultSet.getString("type")));
                    transaction.setAmount(resultSet.getDouble("amount"));
                    transaction.setDescription(safe(resultSet.getString("description")));
                    transaction.setCreatedAt(safe(resultSet.getString("created_at")));

                    transactionList.add(transaction);
                }
            }
        }

        return transactionList;
    }

    public VendorWallet withdraw(int vendorId, double amount) throws SQLException {
        if (amount <= 0) {
            throw new SQLException("Invalid withdraw amount.");
        }

        syncPaidPaymentsToWallet(vendorId);
        rebuildWalletBalance(vendorId);

        String selectWalletSql =
                "SELECT IFNULL(balance, 0.00) AS balance " +
                "FROM vendor_wallet " +
                "WHERE vendor_id = ? " +
                "LIMIT 1 " +
                "FOR UPDATE";

        String insertTransactionSql =
                "INSERT INTO vendor_wallet_transaction " +
                "(vendor_id, booking_id, type, amount, description, created_at) " +
                "VALUES (?, NULL, 'WITHDRAW', ?, ?, NOW())";

        String updateWalletSql =
                "UPDATE vendor_wallet " +
                "SET balance = ?, updated_at = NOW() " +
                "WHERE vendor_id = ?";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                ensureWalletRow(connection, vendorId);

                double currentBalance = 0.00;

                try (PreparedStatement selectStatement = connection.prepareStatement(selectWalletSql)) {
                    selectStatement.setInt(1, vendorId);

                    try (ResultSet resultSet = selectStatement.executeQuery()) {
                        if (resultSet.next()) {
                            currentBalance = resultSet.getDouble("balance");
                        }
                    }
                }

                if (amount > currentBalance) {
                    throw new SQLException("Insufficient wallet balance.");
                }

                try (PreparedStatement insertStatement = connection.prepareStatement(insertTransactionSql)) {
                    insertStatement.setInt(1, vendorId);
                    insertStatement.setDouble(2, amount);
                    insertStatement.setString(3, "Wallet withdrawal");
                    insertStatement.executeUpdate();
                }

                try (PreparedStatement updateStatement = connection.prepareStatement(updateWalletSql)) {
                    updateStatement.setDouble(1, currentBalance - amount);
                    updateStatement.setInt(2, vendorId);
                    updateStatement.executeUpdate();
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }

        return getWalletSummary(vendorId);
    }

    private void syncPaidPaymentsToWallet(int vendorId) throws SQLException {
        String paidPaymentSql =
                "SELECT " +
                "p.booking_id, " +
                "SUM(CASE " +
                "WHEN IFNULL(p.payment_paid, 0.00) > 0.00 THEN p.payment_paid " +
                "WHEN IFNULL(p.final_amount, 0.00) > 0.00 THEN p.final_amount " +
                "ELSE IFNULL(p.amount_original, 0.00) END) AS paid_amount " +
                "FROM payment p " +
                "INNER JOIN booking b ON p.booking_id = b.id " +
                "INNER JOIN service s ON b.service_id = s.id " +
                "WHERE s.vendor_id = ? " +
                "AND p.booking_id IS NOT NULL " +
                "AND LOWER(TRIM(IFNULL(p.payment_status, ''))) IN ('paid', 'success', 'successful', 'completed', 'settled') " +
                "GROUP BY p.booking_id";

        String creditedSql =
                "SELECT IFNULL(SUM(amount), 0.00) AS credited_amount " +
                "FROM vendor_wallet_transaction " +
                "WHERE vendor_id = ? " +
                "AND booking_id = ? " +
                "AND UPPER(type) = 'CREDIT'";

        String insertTransactionSql =
                "INSERT INTO vendor_wallet_transaction " +
                "(vendor_id, booking_id, type, amount, description, created_at) " +
                "VALUES (?, ?, 'CREDIT', ?, ?, NOW())";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                ensureWalletRow(connection, vendorId);

                try (PreparedStatement paidStatement = connection.prepareStatement(paidPaymentSql)) {
                    paidStatement.setInt(1, vendorId);

                    try (ResultSet paidResult = paidStatement.executeQuery()) {
                        while (paidResult.next()) {
                            int bookingId = paidResult.getInt("booking_id");
                            double paidAmount = paidResult.getDouble("paid_amount");

                            if (paidAmount <= 0) {
                                continue;
                            }

                            double creditedAmount = 0.00;

                            try (PreparedStatement creditedStatement = connection.prepareStatement(creditedSql)) {
                                creditedStatement.setInt(1, vendorId);
                                creditedStatement.setInt(2, bookingId);

                                try (ResultSet creditedResult = creditedStatement.executeQuery()) {
                                    if (creditedResult.next()) {
                                        creditedAmount = creditedResult.getDouble("credited_amount");
                                    }
                                }
                            }

                            double amountToCredit = paidAmount - creditedAmount;

                            if (amountToCredit > 0.009) {
                                try (PreparedStatement insertStatement = connection.prepareStatement(insertTransactionSql)) {
                                    insertStatement.setInt(1, vendorId);
                                    insertStatement.setInt(2, bookingId);
                                    insertStatement.setDouble(3, amountToCredit);
                                    insertStatement.setString(4, "Payment received for Booking #" + bookingId);
                                    insertStatement.executeUpdate();
                                }
                            }
                        }
                    }
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

    private void rebuildWalletBalance(int vendorId) throws SQLException {
        String balanceSql =
                "SELECT " +
                "IFNULL(SUM(CASE WHEN UPPER(type) = 'CREDIT' THEN amount ELSE 0 END), 0.00) - " +
                "IFNULL(SUM(CASE WHEN UPPER(type) = 'WITHDRAW' THEN amount ELSE 0 END), 0.00) AS balance " +
                "FROM vendor_wallet_transaction " +
                "WHERE vendor_id = ?";

        String updateWalletSql =
                "UPDATE vendor_wallet " +
                "SET balance = ?, updated_at = NOW() " +
                "WHERE vendor_id = ?";

        try (Connection connection = getConnection()) {
            ensureWalletRow(connection, vendorId);

            double balance = 0.00;

            try (PreparedStatement balanceStatement = connection.prepareStatement(balanceSql)) {
                balanceStatement.setInt(1, vendorId);

                try (ResultSet resultSet = balanceStatement.executeQuery()) {
                    if (resultSet.next()) {
                        balance = resultSet.getDouble("balance");
                    }
                }
            }

            if (balance < 0) {
                balance = 0.00;
            }

            try (PreparedStatement updateStatement = connection.prepareStatement(updateWalletSql)) {
                updateStatement.setDouble(1, balance);
                updateStatement.setInt(2, vendorId);
                updateStatement.executeUpdate();
            }
        }
    }

    private void ensureWalletRow(Connection connection, int vendorId) throws SQLException {
        String checkSql =
                "SELECT id " +
                "FROM vendor_wallet " +
                "WHERE vendor_id = ? " +
                "LIMIT 1";

        String insertSql =
                "INSERT INTO vendor_wallet " +
                "(vendor_id, balance, created_at, updated_at) " +
                "VALUES (?, 0.00, NOW(), NOW())";

        boolean exists = false;

        try (PreparedStatement checkStatement = connection.prepareStatement(checkSql)) {
            checkStatement.setInt(1, vendorId);

            try (ResultSet resultSet = checkStatement.executeQuery()) {
                if (resultSet.next()) {
                    exists = true;
                }
            }
        }

        if (!exists) {
            try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                insertStatement.setInt(1, vendorId);
                insertStatement.executeUpdate();
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