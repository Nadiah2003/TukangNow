package DAO;

import Config.DB_TukangNow;
import Model.HomeVendorData;
import Model.HomeVendorInfo;
import Model.VendorJob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class HomeVendorDAO {

    protected Connection getConnection() throws SQLException {
        Connection connection = DB_TukangNow.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public HomeVendorData getDashboardData(int vendorId) throws SQLException {
        HomeVendorData data = new HomeVendorData();

        data.setStatus("success");
        data.setVendor(getVendorInfo(vendorId));
        data.setUrgentJobs(getUrgentJobs(vendorId));
        data.setPendingJobs(getPendingJobs(vendorId));
        data.setActiveJobs(getActiveJobs(vendorId));
        data.setUnreadNotificationsCount(0);

        return data;
    }

    private HomeVendorInfo getVendorInfo(int vendorId) throws SQLException {
        HomeVendorInfo vendor = new HomeVendorInfo();

        vendor.setName("Vendor");
        vendor.setAccountStatus("Inactive");
        vendor.setProfileImage("");
        vendor.setIsFirstLogin(1);
        vendor.setExpiryInfo("No expiry date");
        vendor.setWalletBalance("0.00");
        vendor.setTotalFinish(0);
        vendor.setAvgRating(0.0);
        vendor.setQualityScore("100%");

        boolean vendorFound = false;

        String sqlVendor = "SELECT name, status, expireddate, profile_path, is_first_login " +
                "FROM vendor " +
                "WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sqlVendor)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    vendorFound = true;
                    vendor.setName(resultSet.getString("name"));
                    vendor.setAccountStatus(resultSet.getString("status"));
                    vendor.setProfileImage(resultSet.getString("profile_path"));
                    vendor.setIsFirstLogin(resultSet.getInt("is_first_login"));

                    String expiredDate = resultSet.getString("expireddate");

                    if (expiredDate != null && !expiredDate.trim().isEmpty()) {
                        vendor.setExpiryInfo("Expires on " + expiredDate);
                    } else {
                        vendor.setExpiryInfo("No expiry date");
                    }
                }
            }
        }

        if (vendorFound && vendor.getIsFirstLogin() == 1) {
            if (isServiceSetupComplete(vendorId)) {
                updateFirstLoginStatus(vendorId);
                vendor.setIsFirstLogin(0);
            } else {
                vendor.setIsFirstLogin(1);
            }
        }

        int totalFinish = getTotalFinish(vendorId);
        double monthlyIncome = getMonthlyIncome(vendorId);
        double avgRating = getAverageRating(vendorId);
        int qualityScore;

        if (totalFinish > 0 || avgRating > 0) {
            double ratingPart = (avgRating / 5.0) * 70.0;
            double workPart = Math.min(totalFinish * 5.0, 30.0);
            qualityScore = (int) Math.round(ratingPart + workPart);
        } else {
            qualityScore = 100;
        }

        vendor.setWalletBalance(String.format("%.2f", monthlyIncome));
        vendor.setTotalFinish(totalFinish);
        vendor.setAvgRating(avgRating);
        vendor.setQualityScore(qualityScore + "%");

        return vendor;
    }

    private boolean isServiceSetupComplete(int vendorId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total_count, " +
                "SUM(CASE WHEN subservice IS NOT NULL " +
                "AND TRIM(CAST(subservice AS CHAR)) <> '' " +
                "AND startprice IS NOT NULL " +
                "AND CAST(NULLIF(TRIM(CAST(startprice AS CHAR)), '') AS DECIMAL(10,2)) > 0 " +
                "AND avail_date IS NOT NULL " +
                "AND TRIM(CAST(avail_date AS CHAR)) <> '' " +
                "AND avail_time IS NOT NULL " +
                "AND TRIM(CAST(avail_time AS CHAR)) <> '' " +
                "THEN 1 ELSE 0 END) AS complete_count " +
                "FROM service " +
                "WHERE vendor_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int totalCount = resultSet.getInt("total_count");
                    int completeCount = resultSet.getInt("complete_count");

                    return totalCount > 0 && totalCount == completeCount;
                }
            }
        }

        return false;
    }

    private void updateFirstLoginStatus(int vendorId) throws SQLException {
        String sql = "UPDATE vendor SET is_first_login = 0 WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);
            preparedStatement.executeUpdate();
        }
    }

    private int getTotalFinish(int vendorId) throws SQLException {
        String sql = "SELECT COUNT(b.id) AS total_finish " +
                "FROM booking b " +
                "JOIN service s ON b.service_id = s.id " +
                "WHERE s.vendor_id = ? " +
                "AND LOWER(TRIM(b.status)) IN ('completed', 'rated')";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total_finish");
                }
            }
        }

        return 0;
    }

    private double getMonthlyIncome(int vendorId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(COALESCE(b.totalamount, 0.00) + COALESCE(material.material_total, 0.00)), 0.00) AS monthly_income " +
                "FROM booking b " +
                "JOIN service s ON b.service_id = s.id " +
                "LEFT JOIN ( " +
                "SELECT booking_id, SUM(COALESCE(quantity, 0) * COALESCE(price, 0.00)) AS material_total " +
                "FROM booking_material_items " +
                "GROUP BY booking_id " +
                ") material ON material.booking_id = b.id " +
                "WHERE s.vendor_id = ? " +
                "AND LOWER(TRIM(b.status)) IN ('completed', 'rated') " +
                "AND YEAR(b.bookingdate) = YEAR(CURDATE()) " +
                "AND MONTH(b.bookingdate) = MONTH(CURDATE())";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("monthly_income");
                }
            }
        }

        return 0.00;
    }

    private double getAverageRating(int vendorId) throws SQLException {
        String sql = "SELECT AVG(r.rating_val) AS avg_rating " +
                "FROM rating r " +
                "JOIN booking b ON r.booking_id = b.id " +
                "JOIN service s ON b.service_id = s.id " +
                "WHERE s.vendor_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Math.round(resultSet.getDouble("avg_rating") * 10.0) / 10.0;
                }
            }
        }

        return 0.0;
    }

    private ArrayList<VendorJob> getUrgentJobs(int vendorId) throws SQLException {
        ArrayList<VendorJob> urgentJobs = new ArrayList<>();

        String sql = "SELECT DISTINCT b.id AS bookingId, s.servicename, b.subservicebooked, b.problem, b.deposit, c.name AS custName, b.bookingdate, b.distancekm, b.status " +
                "FROM emergency_booking_services ebs " +
                "JOIN booking b ON ebs.booking_id = b.id " +
                "JOIN service s ON ebs.service_id = s.id " +
                "JOIN customer c ON b.customer_id = c.id " +
                "WHERE s.vendor_id = ? " +
                "AND LOWER(TRIM(b.status)) = 'emergency' " +
                "AND LOWER(TRIM(ebs.notification_status)) = 'pending' " +
                "ORDER BY b.bookingdate DESC";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    urgentJobs.add(buildVendorJob(resultSet));
                }
            }
        }

        return urgentJobs;
    }

    private ArrayList<VendorJob> getPendingJobs(int vendorId) throws SQLException {
        ArrayList<VendorJob> pendingJobs = new ArrayList<>();

        String sql = "SELECT s.servicename, b.subservicebooked, b.problem, b.bookingdate, b.deposit, c.name AS custName, b.id AS bookingId, b.distancekm, b.status " +
                "FROM booking b " +
                "JOIN service s ON b.service_id = s.id " +
                "JOIN customer c ON b.customer_id = c.id " +
                "WHERE s.vendor_id = ? " +
                "AND LOWER(TRIM(b.status)) = 'pending' " +
                "AND LOWER(TRIM(IFNULL(b.subservicebooked, ''))) <> 'emergency' " +
                "ORDER BY b.bookingdate ASC";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    pendingJobs.add(buildVendorJob(resultSet));
                }
            }
        }

        return pendingJobs;
    }

    private ArrayList<VendorJob> getActiveJobs(int vendorId) throws SQLException {
        ArrayList<VendorJob> activeJobs = new ArrayList<>();

        String sql = "SELECT s.servicename, b.subservicebooked, b.problem, b.bookingdate, b.deposit, c.name AS custName, b.id AS bookingId, b.distancekm, b.status " +
                "FROM booking b " +
                "JOIN service s ON b.service_id = s.id " +
                "JOIN customer c ON b.customer_id = c.id " +
                "WHERE s.vendor_id = ? " +
                "AND LOWER(TRIM(b.status)) IN ('accepted', 'on the way', 'arrived', 'started', 'completed') " +
                "ORDER BY b.bookingdate ASC";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    activeJobs.add(buildVendorJob(resultSet));
                }
            }
        }

        return activeJobs;
    }

    private VendorJob buildVendorJob(ResultSet resultSet) throws SQLException {
        VendorJob job = new VendorJob();

        Timestamp bookingTimestamp = resultSet.getTimestamp("bookingdate");

        job.setBookingId(resultSet.getInt("bookingId"));
        job.setTitle(resultSet.getString("servicename"));
        job.setSubservice(resultSet.getString("subservicebooked"));
        job.setProblem(resultSet.getString("problem"));
        job.setDeposit(resultSet.getDouble("deposit"));
        job.setFullSchedule(resultSet.getString("bookingdate"));
        job.setCustName(resultSet.getString("custName"));
        job.setStatus(resultSet.getString("status"));
        job.setTimeAgo(getTimeAgo(bookingTimestamp));

        String distance = resultSet.getString("distancekm");
        job.setDistanceKM(distance == null || distance.trim().isEmpty() ? "-" : distance);

        return job;
    }

    public String respondNow(int vendorId, int bookingId) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                int emergencyServiceId = getEmergencyServiceIdForVendor(connection, vendorId, bookingId);

                if (emergencyServiceId > 0) {
                    boolean accepted = acceptEmergencyBooking(connection, vendorId, bookingId, emergencyServiceId);

                    if (accepted) {
                        connection.commit();
                        return "success";
                    }

                    connection.rollback();
                    return "Booking already taken or unavailable.";
                }

                boolean normalAccepted = acceptNormalPendingBooking(connection, vendorId, bookingId);

                if (normalAccepted) {
                    connection.commit();
                    return "success";
                }

                connection.rollback();
                return "Booking already taken or unavailable.";
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public String rejectBooking(int vendorId, int bookingId) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                boolean emergencyRejected = rejectEmergencyBooking(connection, vendorId, bookingId);

                if (emergencyRejected) {
                    connection.commit();
                    return "success";
                }

                BookingRefundInfo refundInfo = getNormalPendingBookingRefundInfo(connection, vendorId, bookingId);

                if (refundInfo == null) {
                    connection.rollback();
                    return "Booking already updated or unavailable.";
                }

                boolean normalRejected = rejectNormalPendingBooking(connection, vendorId, bookingId);

                if (!normalRejected) {
                    connection.rollback();
                    return "Booking already updated or unavailable.";
                }

                refundCustomerDeposit(connection, refundInfo);

                connection.commit();
                return "success";
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private int getEmergencyServiceIdForVendor(Connection connection, int vendorId, int bookingId) throws SQLException {
        String sql = "SELECT ebs.service_id " +
                "FROM emergency_booking_services ebs " +
                "JOIN service s ON ebs.service_id = s.id " +
                "JOIN booking b ON ebs.booking_id = b.id " +
                "WHERE ebs.booking_id = ? " +
                "AND s.vendor_id = ? " +
                "AND LOWER(TRIM(b.status)) = 'emergency' " +
                "AND LOWER(TRIM(ebs.notification_status)) = 'pending' " +
                "LIMIT 1 FOR UPDATE";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("service_id");
                }
            }
        }

        return 0;
    }

    private boolean acceptEmergencyBooking(Connection connection, int vendorId, int bookingId, int serviceId) throws SQLException {
        String updateBookingSql = "UPDATE booking " +
                "SET service_id = ?, status = 'Accepted' " +
                "WHERE id = ? " +
                "AND LOWER(TRIM(status)) = 'emergency'";

        try (PreparedStatement preparedStatement = connection.prepareStatement(updateBookingSql)) {
            preparedStatement.setInt(1, serviceId);
            preparedStatement.setInt(2, bookingId);

            int updated = preparedStatement.executeUpdate();

            if (updated == 0) {
                return false;
            }
        }

        String acceptSql = "UPDATE emergency_booking_services ebs " +
                "JOIN service s ON ebs.service_id = s.id " +
                "SET ebs.notification_status = 'accepted', ebs.responded_at = NOW() " +
                "WHERE ebs.booking_id = ? " +
                "AND s.vendor_id = ? " +
                "AND ebs.service_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(acceptSql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, vendorId);
            preparedStatement.setInt(3, serviceId);
            preparedStatement.executeUpdate();
        }

        String closeOthersSql = "UPDATE emergency_booking_services " +
                "SET notification_status = 'taken', responded_at = NOW() " +
                "WHERE booking_id = ? " +
                "AND service_id <> ? " +
                "AND LOWER(TRIM(notification_status)) = 'pending'";

        try (PreparedStatement preparedStatement = connection.prepareStatement(closeOthersSql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, serviceId);
            preparedStatement.executeUpdate();
        }

        return true;
    }

    private boolean acceptNormalPendingBooking(Connection connection, int vendorId, int bookingId) throws SQLException {
        String sql = "UPDATE booking b " +
                "JOIN service s ON b.service_id = s.id " +
                "SET b.status = 'Accepted' " +
                "WHERE b.id = ? " +
                "AND s.vendor_id = ? " +
                "AND LOWER(TRIM(b.status)) = 'pending' " +
                "AND LOWER(TRIM(IFNULL(b.subservicebooked, ''))) <> 'emergency'";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, vendorId);

            return preparedStatement.executeUpdate() > 0;
        }
    }

    private boolean rejectEmergencyBooking(Connection connection, int vendorId, int bookingId) throws SQLException {
        String sql = "UPDATE emergency_booking_services ebs " +
                "JOIN service s ON ebs.service_id = s.id " +
                "JOIN booking b ON ebs.booking_id = b.id " +
                "SET ebs.notification_status = 'rejected', ebs.responded_at = NOW() " +
                "WHERE ebs.booking_id = ? " +
                "AND s.vendor_id = ? " +
                "AND LOWER(TRIM(b.status)) = 'emergency' " +
                "AND LOWER(TRIM(ebs.notification_status)) = 'pending'";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, vendorId);

            return preparedStatement.executeUpdate() > 0;
        }
    }

    private BookingRefundInfo getNormalPendingBookingRefundInfo(Connection connection, int vendorId, int bookingId) throws SQLException {
        String sql = "SELECT b.id AS booking_id, b.customer_id, COALESCE(b.deposit, 0.00) AS deposit_amount " +
                "FROM booking b " +
                "JOIN service s ON b.service_id = s.id " +
                "WHERE b.id = ? " +
                "AND s.vendor_id = ? " +
                "AND LOWER(TRIM(b.status)) = 'pending' " +
                "AND LOWER(TRIM(IFNULL(b.subservicebooked, ''))) <> 'emergency' " +
                "LIMIT 1 FOR UPDATE";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    BookingRefundInfo refundInfo = new BookingRefundInfo();
                    refundInfo.bookingId = resultSet.getInt("booking_id");
                    refundInfo.customerId = resultSet.getInt("customer_id");
                    refundInfo.amount = resultSet.getDouble("deposit_amount");
                    return refundInfo;
                }
            }
        }

        return null;
    }

    private boolean rejectNormalPendingBooking(Connection connection, int vendorId, int bookingId) throws SQLException {
        String sql = "UPDATE booking b " +
                "JOIN service s ON b.service_id = s.id " +
                "SET b.status = 'Reject' " +
                "WHERE b.id = ? " +
                "AND s.vendor_id = ? " +
                "AND LOWER(TRIM(b.status)) = 'pending' " +
                "AND LOWER(TRIM(IFNULL(b.subservicebooked, ''))) <> 'emergency'";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, vendorId);

            return preparedStatement.executeUpdate() > 0;
        }
    }

    private void refundCustomerDeposit(Connection connection, BookingRefundInfo refundInfo) throws SQLException {
        if (refundInfo == null || refundInfo.customerId <= 0 || refundInfo.amount <= 0) {
            return;
        }

        Integer walletId = getCustomerWalletId(connection, refundInfo.customerId);

        if (walletId == null) {
            insertCustomerWallet(connection, refundInfo.customerId, refundInfo.amount);
        } else {
            updateCustomerWallet(connection, walletId, refundInfo.amount);
        }

        insertWalletTransaction(connection, refundInfo.customerId, refundInfo.amount, refundInfo.bookingId);
    }

    private Integer getCustomerWalletId(Connection connection, int customerId) throws SQLException {
        String sql = "SELECT id " +
                "FROM customer_wallet " +
                "WHERE customer_id = ? " +
                "LIMIT 1 FOR UPDATE";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }

        return null;
    }

    private void insertCustomerWallet(Connection connection, int customerId, double amount) throws SQLException {
        String sql = "INSERT INTO customer_wallet " +
                "(customer_id, balance, updated_at) " +
                "VALUES (?, ?, NOW())";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);
            preparedStatement.setDouble(2, amount);
            preparedStatement.executeUpdate();
        }
    }

    private void updateCustomerWallet(Connection connection, int walletId, double amount) throws SQLException {
        String sql = "UPDATE customer_wallet " +
                "SET balance = COALESCE(balance, 0.00) + ?, updated_at = NOW() " +
                "WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDouble(1, amount);
            preparedStatement.setInt(2, walletId);
            preparedStatement.executeUpdate();
        }
    }

    private void insertWalletTransaction(Connection connection, int customerId, double amount, int bookingId) throws SQLException {
        String sql = "INSERT INTO wallet_transactions " +
                "(customer_id, amount, type, reference_id, created_at) " +
                "VALUES (?, ?, 'Refund', ?, NOW())";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);
            preparedStatement.setDouble(2, amount);
            preparedStatement.setInt(3, bookingId);
            preparedStatement.executeUpdate();
        }
    }

    private String getTimeAgo(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }

        long diffMillis = System.currentTimeMillis() - timestamp.getTime();

        if (diffMillis < 0) {
            return "Just now";
        }

        long seconds = diffMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day(s) ago";
        }

        if (hours > 0) {
            return hours + " hour(s) ago";
        }

        if (minutes > 0) {
            return minutes + " minute(s) ago";
        }

        return "Just now";
    }

    private static class BookingRefundInfo {
        private int bookingId;
        private int customerId;
        private double amount;
    }
}