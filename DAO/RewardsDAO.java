package DAO;

import Config.DB_TukangNow;
import Model.CustomerVoucherItem;
import Model.PointHistoryItem;
import Model.RedeemRewardResult;
import Model.RewardCatalogItem;
import Model.RewardsCustomer;
import Model.RewardsPageData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RewardsDAO {

    private Connection getConnection() throws SQLException {
        Connection connection = DB_TukangNow.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public RewardsPageData getRewardsPageData(int customerId) throws SQLException {
        RewardsPageData data = new RewardsPageData();

        try (Connection connection = getConnection()) {
            data.setStatus("success");
            data.setMessage("Rewards loaded.");
            data.setCustomer(getCustomer(connection, customerId));
            data.setRewards(getRewardsCatalog(connection));
            data.setMy_vouchers(getMyVouchers(connection, customerId));
            data.setHistory_vouchers(getPointHistory(connection, customerId));
        }

        return data;
    }

    public RedeemRewardResult redeemReward(int customerId, int rewardId) throws SQLException {
        RedeemRewardResult result = new RedeemRewardResult();

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                RewardsCustomer customer = getCustomerForUpdate(connection, customerId);

                if (customer == null) {
                    throw new SQLException("Customer record not found.");
                }

                RewardCatalogItem reward = getRewardByIdForUpdate(connection, rewardId);

                if (reward == null) {
                    throw new SQLException("Voucher template not found.");
                }

                if (customer.getRewards_points() < reward.getPoints_required()) {
                    throw new SQLException("Insufficient reward points match.");
                }

                deductCustomerPoints(connection, customerId, reward.getPoints_required());
                insertCustomerVoucher(connection, customerId, rewardId);
                insertPointHistory(connection, customerId, "Redeemed " + reward.getVoucher_name(), -reward.getPoints_required());

                connection.commit();

                result.setStatus("success");
                result.setMessage("Voucher '" + reward.getVoucher_name() + "' has been successfully added to your account wallet.");

                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private RewardsCustomer getCustomer(Connection connection, int customerId) throws SQLException {
        String sql = "SELECT name, rewards_points FROM customer WHERE id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    RewardsCustomer customer = new RewardsCustomer();
                    customer.setName(resultSet.getString("name"));
                    customer.setRewards_points(resultSet.getInt("rewards_points"));
                    return customer;
                }
            }
        }

        RewardsCustomer customer = new RewardsCustomer();
        customer.setName("Customer");
        customer.setRewards_points(0);
        return customer;
    }

    private RewardsCustomer getCustomerForUpdate(Connection connection, int customerId) throws SQLException {
        String sql = "SELECT name, rewards_points FROM customer WHERE id = ? LIMIT 1 FOR UPDATE";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    RewardsCustomer customer = new RewardsCustomer();
                    customer.setName(resultSet.getString("name"));
                    customer.setRewards_points(resultSet.getInt("rewards_points"));
                    return customer;
                }
            }
        }

        return null;
    }

    private List<RewardCatalogItem> getRewardsCatalog(Connection connection) throws SQLException {
        List<RewardCatalogItem> rewards = new ArrayList<>();

        String sql = "SELECT id, voucher_name, points_required, discount_amount FROM rewards ORDER BY points_required ASC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                RewardCatalogItem reward = new RewardCatalogItem();
                reward.setId(resultSet.getInt("id"));
                reward.setVoucher_name(resultSet.getString("voucher_name"));
                reward.setPoints_required(resultSet.getInt("points_required"));
                reward.setDiscount_amount(resultSet.getDouble("discount_amount"));
                rewards.add(reward);
            }
        }

        return rewards;
    }

    private RewardCatalogItem getRewardByIdForUpdate(Connection connection, int rewardId) throws SQLException {
        String sql = "SELECT id, voucher_name, points_required, discount_amount FROM rewards WHERE id = ? LIMIT 1 FOR UPDATE";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, rewardId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    RewardCatalogItem reward = new RewardCatalogItem();
                    reward.setId(resultSet.getInt("id"));
                    reward.setVoucher_name(resultSet.getString("voucher_name"));
                    reward.setPoints_required(resultSet.getInt("points_required"));
                    reward.setDiscount_amount(resultSet.getDouble("discount_amount"));
                    return reward;
                }
            }
        }

        return null;
    }

    private List<CustomerVoucherItem> getMyVouchers(Connection connection, int customerId) throws SQLException {
        List<CustomerVoucherItem> vouchers = new ArrayList<>();

        String sql = "SELECT cv.id, DATE_FORMAT(cv.redeemed_at, '%d %b %Y') AS redeemed_at, r.voucher_name, r.discount_amount " +
                "FROM customer_vouchers cv " +
                "INNER JOIN rewards r ON cv.voucher_id = r.id " +
                "WHERE cv.customer_id = ? AND cv.status = 'unused' " +
                "ORDER BY cv.id DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    CustomerVoucherItem voucher = new CustomerVoucherItem();
                    voucher.setId(resultSet.getInt("id"));
                    voucher.setRedeemed_at(resultSet.getString("redeemed_at"));
                    voucher.setVoucher_name(resultSet.getString("voucher_name"));
                    voucher.setDiscount_amount(resultSet.getDouble("discount_amount"));
                    vouchers.add(voucher);
                }
            }
        }

        return vouchers;
    }

    private List<PointHistoryItem> getPointHistory(Connection connection, int customerId) throws SQLException {
        List<PointHistoryItem> history = new ArrayList<>();

        String sql = "SELECT activity, points, DATE_FORMAT(created_at, '%d %b %Y, %h:%i %p') AS date_created " +
                "FROM point_history " +
                "WHERE customer_id = ? " +
                "ORDER BY id DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    PointHistoryItem item = new PointHistoryItem();
                    item.setActivity(resultSet.getString("activity"));
                    item.setPoints(resultSet.getInt("points"));
                    item.setDate_created(resultSet.getString("date_created"));
                    history.add(item);
                }
            }
        }

        return history;
    }

    private void deductCustomerPoints(Connection connection, int customerId, int pointsRequired) throws SQLException {
        String sql = "UPDATE customer SET rewards_points = rewards_points - ? WHERE id = ? AND rewards_points >= ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, pointsRequired);
            preparedStatement.setInt(2, customerId);
            preparedStatement.setInt(3, pointsRequired);

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Failed to deduct reward points.");
            }
        }
    }

    private void insertCustomerVoucher(Connection connection, int customerId, int rewardId) throws SQLException {
        String sql = "INSERT INTO customer_vouchers (customer_id, voucher_id, status, redeemed_at) VALUES (?, ?, 'unused', NOW())";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);
            preparedStatement.setInt(2, rewardId);

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Failed to create customer voucher.");
            }
        }
    }

    private void insertPointHistory(Connection connection, int customerId, String activity, int points) throws SQLException {
        String sql = "INSERT INTO point_history (customer_id, activity, points, created_at) VALUES (?, ?, ?, NOW())";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);
            preparedStatement.setString(2, activity);
            preparedStatement.setInt(3, points);
            preparedStatement.executeUpdate();
        }
    }
}