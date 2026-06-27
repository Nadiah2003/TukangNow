package DAO;

import Config.DB_TukangNow;
import Model.PaymentInitialData;
import Model.PaymentProcessResult;
import Model.PaymentRequestData;
import Model.PaymentVoucher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    private static final int CANCEL_REWARD_POINTS = 1000;
    private static final double REWARD_MINIMUM_PAYMENT = 100.00;
    private static final int REWARD_POINTS_PER_RINGGIT = 1;

    public Connection getConnection() throws SQLException {
        Connection connection = DB_TukangNow.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public PaymentInitialData getInitialData(int customerId, int bookingId) throws SQLException {
        PaymentInitialData data = new PaymentInitialData();

        try (Connection connection = getConnection()) {
            data.setStatus("success");
            data.setMessage("Payment data loaded.");
            data.setPayment_type(bookingId > 0 && isBookingSecondPayment(connection, bookingId, customerId) ? "second_payment" : "first_payment");
            data.setRewards_points(getRewardsPoints(connection, customerId));
            data.setWallet_balance(getWalletBalance(connection, customerId));
            data.setVouchers(getAvailableVouchers(connection, customerId));
        }

        return data;
    }

    public int calculatePointsDeducted(Connection connection, int customerId, int usePoints, double amountOriginal) throws SQLException {
        if (usePoints != 1) {
            return 0;
        }

        int currentPoints = getRewardsPoints(connection, customerId);
        int maxPointsNeeded = (int) Math.round(amountOriginal * 100.0);

        return Math.min(currentPoints, maxPointsNeeded);
    }

    public PaymentProcessResult processWalletPayment(PaymentRequestData requestData, String generatedOrderId) throws SQLException {
        PaymentProcessResult result = new PaymentProcessResult();

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                normalizeRequestPaymentType(connection, requestData);
                requestData.setPaymentMethod("e-wallet");

                if (isSecondPayment(requestData)) {
                    PaymentProcessResult secondResult = processSecondPaymentWallet(connection, requestData, generatedOrderId);
                    connection.commit();
                    return secondResult;
                }

                double walletBalance = getWalletBalanceForUpdate(connection, requestData.getCustomerId());

                if (walletBalance < requestData.getFinalAmount()) {
                    throw new SQLException("Baki e-wallet tidak mencukupi untuk membuat pembayaran.");
                }

                int pointsDeducted = calculatePointsDeducted(connection, requestData.getCustomerId(), requestData.getUsePoints(), requestData.getAmountOriginal());

                deductWallet(connection, requestData.getCustomerId(), requestData.getFinalAmount());
                insertWalletTransaction(connection, requestData.getCustomerId(), requestData.getFinalAmount(), "payment", generatedOrderId);

                deductRewardsPoints(connection, requestData.getCustomerId(), pointsDeducted);
                markVoucherUsed(connection, requestData.getCustomerVoucherId(), requestData.getCustomerId());

                int earnedPoints = calculateEarnedRewardPoints(requestData.getFinalAmount());
                addRewardPoints(connection, requestData.getCustomerId(), earnedPoints);

                int bookingId;
                String bookingStatus = getBookingStatusAfterSuccessfulPayment(requestData);

                if (requestData.getBookingId() <= 0) {
                    bookingId = insertBooking(connection, requestData, bookingStatus);
                } else {
                    bookingId = requestData.getBookingId();
                    updateBookingStatus(connection, bookingId, requestData.getCustomerId(), bookingStatus);
                }

                insertPaymentRecord(connection, bookingId, requestData, "paid", "e-wallet", requestData.getFinalAmount(), generatedOrderId, generatedOrderId, "", "TukangNow Wallet", "success", "", pointsDeducted, earnedPoints, true, false);

                connection.commit();

                result.setStatus("success");
                result.setMessage("Pembayaran e-wallet berjaya. Booking dicipta.");
                result.setPaymentType("first_payment");
                result.setOrderId(generatedOrderId);
                result.setTransactionId(generatedOrderId);
                result.setWalletTransactionId(generatedOrderId);
                result.setMethod("e-wallet");
                result.setBookingId(bookingId);

                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private PaymentProcessResult processSecondPaymentWallet(Connection connection, PaymentRequestData requestData, String generatedOrderId) throws SQLException {
        if (requestData.getBookingId() <= 0) {
            throw new SQLException("Booking ID is required for second payment.");
        }

        double walletBalance = getWalletBalanceForUpdate(connection, requestData.getCustomerId());

        if (walletBalance < requestData.getFinalAmount()) {
            throw new SQLException("Baki e-wallet tidak mencukupi untuk membuat pembayaran.");
        }

        deductWallet(connection, requestData.getCustomerId(), requestData.getFinalAmount());
        insertWalletTransaction(connection, requestData.getCustomerId(), requestData.getFinalAmount(), "payment", generatedOrderId);

        int earnedPoints = calculateEarnedRewardPoints(requestData.getFinalAmount());
        addRewardPoints(connection, requestData.getCustomerId(), earnedPoints);

        upsertSecondPaymentRecord(connection, requestData.getBookingId(), requestData, "paid", "e-wallet", requestData.getFinalAmount(), generatedOrderId, generatedOrderId, "", "TukangNow Wallet", "success", "", earnedPoints, true, false);
        updateBookingStatus(connection, requestData.getBookingId(), requestData.getCustomerId(), "Completed");

        PaymentProcessResult result = new PaymentProcessResult();
        result.setStatus("success");
        result.setMessage("Second payment completed successfully.");
        result.setPaymentType("second_payment");
        result.setOrderId(generatedOrderId);
        result.setTransactionId(generatedOrderId);
        result.setWalletTransactionId(generatedOrderId);
        result.setMethod("e-wallet");
        result.setBookingId(requestData.getBookingId());

        return result;
    }

    public PaymentProcessResult prepareFpxPayment(PaymentRequestData requestData, String generatedOrderId) throws SQLException {
        PaymentProcessResult result = new PaymentProcessResult();

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                normalizeRequestPaymentType(connection, requestData);
                requestData.setPaymentMethod("fpx");

                if (isSecondPayment(requestData)) {
                    if (requestData.getBookingId() <= 0) {
                        throw new SQLException("Booking ID is required for second payment.");
                    }

                    upsertSecondPaymentRecord(connection, requestData.getBookingId(), requestData, "pending", "fpx", 0.00, generatedOrderId, "", "", "ToyyibPay", "", "", 0, false, false);

                    connection.commit();

                    result.setStatus("success");
                    result.setMessage("Second payment FPX prepared.");
                    result.setPaymentType("second_payment");
                    result.setOrderId(generatedOrderId);
                    result.setMethod("fpx");
                    result.setBookingId(requestData.getBookingId());

                    return result;
                }

                int pointsDeducted = calculatePointsDeducted(connection, requestData.getCustomerId(), requestData.getUsePoints(), requestData.getAmountOriginal());

                int bookingId;
                String bookingStatus = getBookingStatusAfterSuccessfulPayment(requestData);

                if (requestData.getBookingId() <= 0) {
                    bookingId = insertBooking(connection, requestData, bookingStatus);
                } else {
                    bookingId = requestData.getBookingId();
                    updateBookingStatus(connection, bookingId, requestData.getCustomerId(), bookingStatus);
                }

                insertPaymentRecord(connection, bookingId, requestData, "pending", "fpx", 0.00, generatedOrderId, "", "", "ToyyibPay", "", "", pointsDeducted, 0, false, false);

                connection.commit();

                result.setStatus("success");
                result.setMessage("FPX payment prepared.");
                result.setPaymentType("first_payment");
                result.setOrderId(generatedOrderId);
                result.setMethod("fpx");
                result.setBookingId(bookingId);

                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void updatePaymentReference(int bookingId, String paymentReference) throws SQLException {
        updatePaymentReference(bookingId, paymentReference, "first_payment");
    }

    public void updatePaymentReference(int bookingId, String paymentReference, String paymentType) throws SQLException {
        String sql = "UPDATE payment " +
                "SET gateway_bill_code = ?, payment_reference = ?, updated_at = NOW() " +
                "WHERE booking_id = ? " +
                "AND LOWER(TRIM(payment_type)) = ? " +
                "AND LOWER(TRIM(payment_status)) = 'pending' " +
                "ORDER BY id DESC LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(paymentReference));
            preparedStatement.setString(2, safe(paymentReference));
            preparedStatement.setInt(3, bookingId);
            preparedStatement.setString(4, normalizePaymentType(paymentType));

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Failed to update payment reference.");
            }
        }
    }

    public void markFpxPaymentSuccess(int bookingId, int customerId, double paidAmount, String paymentType) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                String cleanPaymentType = normalizePaymentType(paymentType);

                if (isBookingSecondPayment(connection, bookingId, customerId)) {
                    cleanPaymentType = "second_payment";
                }

                PendingPayment pendingPayment = getLatestPaymentForUpdate(connection, bookingId, customerId, cleanPaymentType);

                if (pendingPayment == null) {
                    throw new SQLException("Payment record not found.");
                }

                if ("paid".equalsIgnoreCase(pendingPayment.paymentStatus)) {
                    if ("second_payment".equals(cleanPaymentType)) {
                        updateBookingStatus(connection, bookingId, customerId, "Completed");
                    } else {
                        updateBookingStatusAfterSuccessfulPayment(connection, bookingId, customerId);
                    }

                    connection.commit();
                    return;
                }

                double finalPaidAmount = paidAmount > 0 ? paidAmount : pendingPayment.finalAmount;
                int earnedPoints = calculateEarnedRewardPoints(finalPaidAmount);

                String updatePaymentSql = "UPDATE payment " +
                        "SET payment_status = 'paid', payment_paid = ?, transaction_id = IFNULL(NULLIF(transaction_id, ''), order_id), reward_points_earned = ?, gateway_response = 'success', failed_reason = '', paid_at = NOW(), failed_at = NULL, updated_at = NOW() " +
                        "WHERE id = ?";

                try (PreparedStatement updatePaymentStatement = connection.prepareStatement(updatePaymentSql)) {
                    updatePaymentStatement.setDouble(1, finalPaidAmount);
                    updatePaymentStatement.setInt(2, earnedPoints);
                    updatePaymentStatement.setInt(3, pendingPayment.paymentId);

                    int affectedRows = updatePaymentStatement.executeUpdate();

                    if (affectedRows == 0) {
                        throw new SQLException("Failed to update payment success.");
                    }
                }

                if ("first_payment".equals(cleanPaymentType)) {
                    deductRewardsPoints(connection, customerId, pendingPayment.rewardPointsUsed);
                    markVoucherUsed(connection, pendingPayment.customerVoucherId, customerId);
                }

                addRewardPoints(connection, customerId, earnedPoints);

                if ("second_payment".equals(cleanPaymentType)) {
                    updateBookingStatus(connection, bookingId, customerId, "Completed");
                } else {
                    updateBookingStatusAfterSuccessfulPayment(connection, bookingId, customerId);
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

    public void markBookingPaymentFailed(int bookingId, int customerId) throws SQLException {
        markBookingPaymentFailed(bookingId, customerId, "first_payment");
    }

    public void markBookingPaymentFailed(int bookingId, int customerId, String paymentType) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                String cleanPaymentType = normalizePaymentType(paymentType);

                if (isBookingSecondPayment(connection, bookingId, customerId)) {
                    cleanPaymentType = "second_payment";
                }

                String updatePaymentSql = "UPDATE payment " +
                        "SET payment_status = 'failed', gateway_response = 'failed', failed_at = NOW(), failed_reason = 'Payment failed or cancelled.', updated_at = NOW() " +
                        "WHERE booking_id = ? " +
                        "AND customer_id = ? " +
                        "AND LOWER(TRIM(payment_type)) = ? " +
                        "AND LOWER(TRIM(payment_status)) = 'pending' " +
                        "ORDER BY id DESC LIMIT 1";

                try (PreparedStatement updatePaymentStatement = connection.prepareStatement(updatePaymentSql)) {
                    updatePaymentStatement.setInt(1, bookingId);
                    updatePaymentStatement.setInt(2, customerId);
                    updatePaymentStatement.setString(3, cleanPaymentType);
                    updatePaymentStatement.executeUpdate();
                }

                if ("first_payment".equals(cleanPaymentType) && !hasPaidPayment(connection, bookingId)) {
                    String updateBookingSql = "UPDATE booking " +
                            "SET status = 'Payment Failed' " +
                            "WHERE id = ? " +
                            "AND customer_id = ?";

                    try (PreparedStatement updateBookingStatement = connection.prepareStatement(updateBookingSql)) {
                        updateBookingStatement.setInt(1, bookingId);
                        updateBookingStatement.setInt(2, customerId);
                        updateBookingStatement.executeUpdate();
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

    public boolean isCustomerPasswordValid(int customerId, String password) throws SQLException {
        if (password == null) {
            return false;
        }

        String storedPassword = getStoredPasswordByCustomerId(customerId);

        if (storedPassword == null) {
            return false;
        }

        return isPasswordMatched(password, storedPassword);
    }

    public String getPasswordFailureMessage(int customerId, String password) throws SQLException {
        if (password == null || password.isEmpty()) {
            return "Password not received by server. Please enter password again.";
        }

        String storedPassword = getStoredPasswordByCustomerId(customerId);

        if (storedPassword == null) {
            return "Current login session customer ID not found in customer table. Please logout and login again.";
        }

        if (isPasswordMatched(password, storedPassword)) {
            return "Password verified.";
        }

        int matchedCustomerId = findCustomerIdByPassword(password);

        if (matchedCustomerId > 0 && matchedCustomerId != customerId) {
            return "Password belongs to another customer account. Please logout and login using the correct customer account.";
        }

        return "Incorrect password. Please make sure you are logged in to the same customer account.";
    }

    private String getStoredPasswordByCustomerId(int customerId) throws SQLException {
        String sql = "SELECT password FROM customer WHERE id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("password");
                }
            }
        }

        return null;
    }

    private int findCustomerIdByPassword(String password) throws SQLException {
        String sql = "SELECT id, password FROM customer";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String storedPassword = resultSet.getString("password");

                if (isPasswordMatched(password, storedPassword)) {
                    return id;
                }
            }
        }

        return 0;
    }

    private boolean isPasswordMatched(String inputPassword, String storedPassword) {
        String inputRaw = inputPassword == null ? "" : inputPassword;
        String storedRaw = storedPassword == null ? "" : storedPassword;

        if (storedRaw.isEmpty()) {
            return false;
        }

        if (inputRaw.equals(storedRaw)) {
            return true;
        }

        if (inputRaw.trim().equals(storedRaw.trim())) {
            return true;
        }

        String normalizedInput = normalizePassword(inputRaw);
        String normalizedStored = normalizePassword(storedRaw);

        return !normalizedInput.isEmpty() && normalizedInput.equals(normalizedStored);
    }

    private String normalizePassword(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\uFEFF", "")
                .replace("\u200B", "")
                .replace("\u200C", "")
                .replace("\u200D", "")
                .replace("\u00A0", "")
                .replaceAll("\\s+", "");
    }

    private int insertBooking(Connection connection, PaymentRequestData requestData, String bookingStatus) throws SQLException {
        String bookingDateTime = buildBookingDateTime(requestData.getDate(), requestData.getTime());

        String sql = "INSERT INTO booking " +
                "(deposit, totalamount, bookingdate, service_id, subservicebooked, problem, status, customer_id, travelfee, distancekm, evidencepath) " +
                "VALUES (?, ?, IFNULL(NULLIF(?, ''), NOW()), ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setDouble(1, requestData.getAmountOriginal());
            preparedStatement.setDouble(2, requestData.getAmountOriginal());
            preparedStatement.setString(3, bookingDateTime);
            preparedStatement.setInt(4, requestData.getServiceId());
            preparedStatement.setString(5, safe(requestData.getSubservice()));
            preparedStatement.setString(6, safe(requestData.getProblem()));
            preparedStatement.setString(7, safe(bookingStatus));
            preparedStatement.setInt(8, requestData.getCustomerId());
            preparedStatement.setDouble(9, requestData.getTravelFee());
            preparedStatement.setDouble(10, requestData.getDistanceKm());
            preparedStatement.setString(11, safe(requestData.getEvidencePath()));

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Failed to create booking.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int bookingId = generatedKeys.getInt(1);

                    if (isEmergencyBooking(requestData)) {
                        insertEmergencyBookingServices(connection, bookingId, requestData.getEmergencyServiceIds(), requestData.getServiceId());
                    }

                    return bookingId;
                }
            }
        }

        throw new SQLException("Failed to get booking ID.");
    }

    private void insertEmergencyBookingServices(Connection connection, int bookingId, String emergencyServiceIds, int primaryServiceId) throws SQLException {
        List<Integer> serviceIds = parseEmergencyServiceIds(emergencyServiceIds);

        if (serviceIds.isEmpty() && primaryServiceId > 0) {
            serviceIds.add(primaryServiceId);
        }

        if (serviceIds.isEmpty()) {
            return;
        }

        String sql = "INSERT IGNORE INTO emergency_booking_services " +
                "(booking_id, service_id, notification_status, notified_at) " +
                "VALUES (?, ?, 'pending', NOW())";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (Integer serviceId : serviceIds) {
                if (serviceId == null || serviceId <= 0) {
                    continue;
                }

                preparedStatement.setInt(1, bookingId);
                preparedStatement.setInt(2, serviceId);
                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();
        }
    }

    private List<Integer> parseEmergencyServiceIds(String emergencyServiceIds) {
        List<Integer> serviceIds = new ArrayList<>();

        String clean = safe(emergencyServiceIds);

        if (clean.isEmpty()) {
            return serviceIds;
        }

        String[] parts = clean.split(",");

        for (String part : parts) {
            int serviceId = parseIntLocal(part);

            if (serviceId <= 0) {
                continue;
            }

            if (!serviceIds.contains(serviceId)) {
                serviceIds.add(serviceId);
            }
        }

        return serviceIds;
    }

    private int parseIntLocal(String value) {
        try {
            if (value != null && !value.trim().isEmpty()) {
                return Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException e) {
            return 0;
        }

        return 0;
    }

    private boolean isEmergencyBooking(PaymentRequestData requestData) {
        return "Emergency".equalsIgnoreCase(safe(requestData.getService()))
                || "Emergency".equalsIgnoreCase(safe(requestData.getSubservice()))
                || "Emergency".equalsIgnoreCase(safe(requestData.getCategory()))
                || !safe(requestData.getEmergencyServiceIds()).isEmpty();
    }

    private String getBookingStatusAfterSuccessfulPayment(PaymentRequestData requestData) {
        if (isEmergencyBooking(requestData)) {
            return "Emergency";
        }

        return "Pending";
    }

    private void updateBookingStatusAfterSuccessfulPayment(Connection connection, int bookingId, int customerId) throws SQLException {
        String bookingStatus = isEmergencyBookingRecord(connection, bookingId, customerId) ? "Emergency" : "Pending";
        updateBookingStatus(connection, bookingId, customerId, bookingStatus);
    }

    private boolean isEmergencyBookingRecord(Connection connection, int bookingId, int customerId) throws SQLException {
        String sql = "SELECT b.status, b.subservicebooked, " +
                "(SELECT COUNT(*) FROM emergency_booking_services ebs WHERE ebs.booking_id = b.id) AS emergency_count " +
                "FROM booking b " +
                "WHERE b.id = ? AND b.customer_id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String status = safe(resultSet.getString("status"));
                    String subservicebooked = safe(resultSet.getString("subservicebooked"));
                    int emergencyCount = resultSet.getInt("emergency_count");

                    return "Emergency".equalsIgnoreCase(status)
                            || "Emergency".equalsIgnoreCase(subservicebooked)
                            || emergencyCount > 0;
                }
            }
        }

        return false;
    }

    private void updateBookingStatus(Connection connection, int bookingId, int customerId, String bookingStatus) throws SQLException {
        String sql = "UPDATE booking SET status = ? WHERE id = ? AND customer_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, safe(bookingStatus));
            preparedStatement.setInt(2, bookingId);
            preparedStatement.setInt(3, customerId);

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Failed to update booking status.");
            }
        }
    }

    private int insertPaymentRecord(Connection connection, int bookingId, PaymentRequestData requestData, String paymentStatus, String paymentMethod, double paymentPaid, String orderId, String transactionId, String gatewayBillCode, String gatewayName, String gatewayResponse, String failedReason, int pointsUsed, int earnedPoints, boolean paidNow, boolean failedNow) throws SQLException {
        DiscountData discountData = calculateDiscountData(connection, requestData, pointsUsed);

        String sql = "INSERT INTO payment " +
                "(booking_id, customer_id, customer_voucher_id, payment_type, payment_method, payment_status, amount_original, points_discount, voucher_discount, final_amount, payment_paid, reward_points_used, reward_points_earned, payment_reference, order_id, transaction_id, gateway_bill_code, gateway_name, gateway_response, failed_reason, paid_at, failed_at, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, IF(? = 1, NOW(), NULL), IF(? = 1, NOW(), NULL), NOW())";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, requestData.getCustomerId());
            setNullableInt(preparedStatement, 3, requestData.getCustomerVoucherId());
            preparedStatement.setString(4, normalizePaymentType(requestData.getPaymentType()));
            preparedStatement.setString(5, normalizePaymentMethod(paymentMethod));
            preparedStatement.setString(6, safe(paymentStatus));
            preparedStatement.setDouble(7, requestData.getAmountOriginal());
            preparedStatement.setDouble(8, discountData.pointsDiscount);
            preparedStatement.setDouble(9, discountData.voucherDiscount);
            preparedStatement.setDouble(10, requestData.getFinalAmount());
            preparedStatement.setDouble(11, paymentPaid);
            preparedStatement.setInt(12, pointsUsed);
            preparedStatement.setInt(13, earnedPoints);
            preparedStatement.setString(14, safe(orderId));
            preparedStatement.setString(15, safe(orderId));
            preparedStatement.setString(16, safe(transactionId));
            preparedStatement.setString(17, safe(gatewayBillCode));
            preparedStatement.setString(18, safe(gatewayName));
            preparedStatement.setString(19, safe(gatewayResponse));
            preparedStatement.setString(20, safe(failedReason));
            preparedStatement.setInt(21, paidNow ? 1 : 0);
            preparedStatement.setInt(22, failedNow ? 1 : 0);

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Failed to create payment record.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Failed to get payment ID.");
    }

    private void upsertSecondPaymentRecord(Connection connection, int bookingId, PaymentRequestData requestData, String paymentStatus, String paymentMethod, double paymentPaid, String orderId, String transactionId, String gatewayBillCode, String gatewayName, String gatewayResponse, String failedReason, int earnedPoints, boolean paidNow, boolean failedNow) throws SQLException {
        requestData.setPaymentType("second_payment");
        requestData.setUsePoints(0);
        requestData.setCustomerVoucherId(0);

        int existingPaymentId = getExistingPaymentIdForUpdate(connection, bookingId, requestData.getCustomerId(), "second_payment");

        if (existingPaymentId <= 0) {
            insertPaymentRecord(connection, bookingId, requestData, paymentStatus, paymentMethod, paymentPaid, orderId, transactionId, gatewayBillCode, gatewayName, gatewayResponse, failedReason, 0, earnedPoints, paidNow, failedNow);
            return;
        }

        String sql = "UPDATE payment SET " +
                "customer_voucher_id = NULL, " +
                "payment_method = ?, " +
                "payment_status = ?, " +
                "amount_original = ?, " +
                "points_discount = 0.00, " +
                "voucher_discount = 0.00, " +
                "final_amount = ?, " +
                "payment_paid = ?, " +
                "reward_points_used = 0, " +
                "reward_points_earned = ?, " +
                "payment_reference = ?, " +
                "order_id = ?, " +
                "transaction_id = ?, " +
                "gateway_bill_code = ?, " +
                "gateway_name = ?, " +
                "gateway_response = ?, " +
                "failed_reason = ?, " +
                "paid_at = IF(? = 1, NOW(), NULL), " +
                "failed_at = IF(? = 1, NOW(), NULL), " +
                "updated_at = NOW() " +
                "WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, normalizePaymentMethod(paymentMethod));
            preparedStatement.setString(2, safe(paymentStatus));
            preparedStatement.setDouble(3, requestData.getAmountOriginal());
            preparedStatement.setDouble(4, requestData.getFinalAmount());
            preparedStatement.setDouble(5, paymentPaid);
            preparedStatement.setInt(6, earnedPoints);
            preparedStatement.setString(7, safe(orderId));
            preparedStatement.setString(8, safe(orderId));
            preparedStatement.setString(9, safe(transactionId));
            preparedStatement.setString(10, safe(gatewayBillCode));
            preparedStatement.setString(11, safe(gatewayName));
            preparedStatement.setString(12, safe(gatewayResponse));
            preparedStatement.setString(13, safe(failedReason));
            preparedStatement.setInt(14, paidNow ? 1 : 0);
            preparedStatement.setInt(15, failedNow ? 1 : 0);
            preparedStatement.setInt(16, existingPaymentId);

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Failed to update second payment record.");
            }
        }
    }

    private int getExistingPaymentIdForUpdate(Connection connection, int bookingId, int customerId, String paymentType) throws SQLException {
        String sql = "SELECT id FROM payment " +
                "WHERE booking_id = ? " +
                "AND customer_id = ? " +
                "AND LOWER(TRIM(payment_type)) = ? " +
                "ORDER BY id DESC LIMIT 1 FOR UPDATE";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, customerId);
            preparedStatement.setString(3, normalizePaymentType(paymentType));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }

        return 0;
    }

    private DiscountData calculateDiscountData(Connection connection, PaymentRequestData requestData, int pointsUsed) throws SQLException {
        DiscountData discountData = new DiscountData();

        if (isSecondPayment(requestData)) {
            discountData.pointsDiscount = 0.00;
            discountData.voucherDiscount = 0.00;
            return discountData;
        }

        discountData.pointsDiscount = Math.min(pointsUsed / 100.0, requestData.getAmountOriginal());

        double remainingAmount = requestData.getAmountOriginal() - discountData.pointsDiscount;

        if (remainingAmount < 0) {
            remainingAmount = 0;
        }

        if (requestData.getCustomerVoucherId() > 0) {
            double percentage = getVoucherDiscountPercentage(connection, requestData.getCustomerVoucherId(), requestData.getCustomerId());
            discountData.voucherDiscount = remainingAmount * (percentage / 100.0);

            if (discountData.voucherDiscount > remainingAmount) {
                discountData.voucherDiscount = remainingAmount;
            }
        }

        return discountData;
    }

    private double getVoucherDiscountPercentage(Connection connection, int customerVoucherId, int customerId) throws SQLException {
        String sql = "SELECT e.discount_percentage " +
                "FROM customer_vouchers cv " +
                "INNER JOIN events e ON cv.voucher_id = e.id " +
                "WHERE cv.id = ? AND cv.customer_id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerVoucherId);
            preparedStatement.setInt(2, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("discount_percentage");
                }
            }
        }

        return 0.00;
    }

    private PendingPayment getLatestPaymentForUpdate(Connection connection, int bookingId, int customerId, String paymentType) throws SQLException {
        String sql = "SELECT id, customer_voucher_id, payment_status, final_amount, reward_points_used " +
                "FROM payment " +
                "WHERE booking_id = ? " +
                "AND customer_id = ? " +
                "AND LOWER(TRIM(payment_type)) = ? " +
                "AND LOWER(TRIM(payment_status)) IN ('pending', 'paid') " +
                "ORDER BY id DESC LIMIT 1 FOR UPDATE";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, customerId);
            preparedStatement.setString(3, normalizePaymentType(paymentType));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    PendingPayment payment = new PendingPayment();
                    payment.paymentId = resultSet.getInt("id");
                    payment.customerVoucherId = resultSet.getInt("customer_voucher_id");
                    payment.paymentStatus = safe(resultSet.getString("payment_status"));
                    payment.finalAmount = resultSet.getDouble("final_amount");
                    payment.rewardPointsUsed = resultSet.getInt("reward_points_used");
                    return payment;
                }
            }
        }

        return null;
    }

    private boolean hasPaidPayment(Connection connection, int bookingId) throws SQLException {
        String sql = "SELECT id FROM payment WHERE booking_id = ? AND LOWER(TRIM(payment_status)) = 'paid' LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void normalizeRequestPaymentType(Connection connection, PaymentRequestData requestData) throws SQLException {
        String paymentType = normalizePaymentType(requestData.getPaymentType());

        if (requestData.getBookingId() > 0 && isBookingSecondPayment(connection, requestData.getBookingId(), requestData.getCustomerId())) {
            paymentType = "second_payment";
        }

        requestData.setPaymentType(paymentType);

        if ("second_payment".equals(paymentType)) {
            requestData.setUsePoints(0);
            requestData.setCustomerVoucherId(0);
        }
    }

    private boolean isSecondPayment(PaymentRequestData requestData) {
        return "second_payment".equals(normalizePaymentType(requestData.getPaymentType()));
    }

    private boolean isBookingSecondPayment(Connection connection, int bookingId, int customerId) throws SQLException {
        String sql = "SELECT status FROM booking WHERE id = ? AND customer_id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String status = safe(resultSet.getString("status")).toLowerCase();
                    return "second payment".equals(status) || "second_payment".equals(status);
                }
            }
        }

        return false;
    }

    private int getRewardsPoints(Connection connection, int customerId) throws SQLException {
        String sql = "SELECT rewards_points FROM customer WHERE id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("rewards_points");
                }
            }
        }

        return 0;
    }

    private double getWalletBalance(Connection connection, int customerId) throws SQLException {
        createWalletIfNotExists(connection, customerId);

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
        createWalletIfNotExists(connection, customerId);

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

    private List<PaymentVoucher> getAvailableVouchers(Connection connection, int customerId) throws SQLException {
        List<PaymentVoucher> vouchers = new ArrayList<>();

        String sql = "SELECT cv.id AS customer_voucher_id, e.title AS event_name, e.discount_code AS voucher_code, e.discount_percentage " +
                "FROM customer_vouchers cv " +
                "INNER JOIN events e ON cv.voucher_id = e.id " +
                "WHERE cv.customer_id = ? AND cv.status = 'unused'";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    PaymentVoucher voucher = new PaymentVoucher();
                    voucher.setCustomer_voucher_id(resultSet.getInt("customer_voucher_id"));
                    voucher.setEvent_name(resultSet.getString("event_name"));
                    voucher.setVoucher_code(resultSet.getString("voucher_code"));
                    voucher.setDiscount_amount(resultSet.getDouble("discount_percentage"));
                    vouchers.add(voucher);
                }
            }
        }

        return vouchers;
    }

    private void createWalletIfNotExists(Connection connection, int customerId) throws SQLException {
        String checkSql = "SELECT id FROM customer_wallet WHERE customer_id = ? LIMIT 1";

        try (PreparedStatement checkStatement = connection.prepareStatement(checkSql)) {
            checkStatement.setInt(1, customerId);

            try (ResultSet resultSet = checkStatement.executeQuery()) {
                if (resultSet.next()) {
                    return;
                }
            }
        }

        String insertSql = "INSERT INTO customer_wallet (customer_id, balance, updated_at) VALUES (?, 0.00, NOW())";

        try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            insertStatement.setInt(1, customerId);
            insertStatement.executeUpdate();
        }
    }

    private void deductWallet(Connection connection, int customerId, double amount) throws SQLException {
        if (amount <= 0) {
            return;
        }

        String sql = "UPDATE customer_wallet SET balance = balance - ?, updated_at = NOW() WHERE customer_id = ? AND balance >= ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setDouble(1, amount);
            preparedStatement.setInt(2, customerId);
            preparedStatement.setDouble(3, amount);

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Gagal mengemas kini baki e-wallet.");
            }
        }
    }

    private void insertWalletTransaction(Connection connection, int customerId, double amount, String type, String referenceId) throws SQLException {
        if (amount <= 0) {
            return;
        }

        String sql = "INSERT INTO wallet_transactions (customer_id, amount, type, reference_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, customerId);
            preparedStatement.setDouble(2, amount);
            preparedStatement.setString(3, safe(type));
            preparedStatement.setString(4, safe(referenceId));
            preparedStatement.executeUpdate();
        }
    }

    private void deductRewardsPoints(Connection connection, int customerId, int pointsDeducted) throws SQLException {
        if (pointsDeducted <= 0) {
            return;
        }

        String sql = "UPDATE customer SET rewards_points = rewards_points - ? WHERE id = ? AND rewards_points >= ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, pointsDeducted);
            preparedStatement.setInt(2, customerId);
            preparedStatement.setInt(3, pointsDeducted);

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Gagal mengemaskini reward points.");
            }
        }
    }

    private void addRewardPoints(Connection connection, int customerId, int rewardPoints) throws SQLException {
        if (rewardPoints <= 0) {
            return;
        }

        String sql = "UPDATE customer SET rewards_points = rewards_points + ? WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, rewardPoints);
            preparedStatement.setInt(2, customerId);

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Gagal menambah reward points.");
            }
        }
    }

    private int calculateEarnedRewardPoints(double paidAmount) {
        if (paidAmount < REWARD_MINIMUM_PAYMENT) {
            return 0;
        }

        return (int) Math.floor(paidAmount) * REWARD_POINTS_PER_RINGGIT;
    }

    private void markVoucherUsed(Connection connection, int voucherId, int customerId) throws SQLException {
        if (voucherId <= 0) {
            return;
        }

        String sql = "UPDATE customer_vouchers SET status = 'used', used_at = NOW() WHERE id = ? AND customer_id = ? AND status = 'unused'";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, voucherId);
            preparedStatement.setInt(2, customerId);
            preparedStatement.executeUpdate();
        }
    }

    private String normalizePaymentMethod(String method) {
        String clean = safe(method).toLowerCase();

        if ("ewallet".equals(clean) || "e-wallet".equals(clean) || "wallet".equals(clean)) {
            return "e-wallet";
        }

        if ("balance".equals(clean)) {
            return "balance";
        }

        return "fpx";
    }

    private String normalizePaymentType(String type) {
        String clean = safe(type).toLowerCase().replace("-", "_").replace(" ", "_");

        if ("second_payment".equals(clean) || "balance".equals(clean)) {
            return "second_payment";
        }

        return "first_payment";
    }

    private String buildBookingDateTime(String date, String time) {
        String cleanDate = safe(date);
        String cleanTime = safe(time);

        if (cleanDate.isEmpty() || cleanTime.isEmpty()) {
            return "";
        }

        return cleanDate + " " + cleanTime;
    }

    private void setNullableInt(PreparedStatement preparedStatement, int index, int value) throws SQLException {
        if (value > 0) {
            preparedStatement.setInt(index, value);
        } else {
            preparedStatement.setNull(index, Types.INTEGER);
        }
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private static class DiscountData {
        private double pointsDiscount;
        private double voucherDiscount;
    }

    private static class PendingPayment {
        private int paymentId;
        private int customerVoucherId;
        private String paymentStatus;
        private double finalAmount;
        private int rewardPointsUsed;
    }
}