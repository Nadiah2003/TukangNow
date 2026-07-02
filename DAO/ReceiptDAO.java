package DAO;

import Config.ConnectionManager;
import Model.ReceiptData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReceiptDAO {

    private Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public ReceiptData getReceiptData(int customerId, String idOrRef) throws SQLException {
        return getReceiptData(customerId, idOrRef, "first_payment");
    }

    public ReceiptData getReceiptData(int customerId, String idOrRef, String paymentType) throws SQLException {
        String searchRef = idOrRef == null ? "" : idOrRef.trim();
        int cleanId = extractBookingId(searchRef);
        String cleanPaymentType = normalizePaymentType(paymentType);

        String sql = "SELECT " +
                "b.id, " +
                "b.bookingdate, " +
                "CASE " +
                "WHEN b.status = 'Emergency' THEN 'Emergency Service' " +
                "WHEN b.subservicebooked IS NOT NULL AND TRIM(b.subservicebooked) <> '' THEN b.subservicebooked " +
                "WHEN s.servicename IS NOT NULL AND TRIM(s.servicename) <> '' THEN s.servicename " +
                "ELSE 'General Service' END AS service_display, " +
                "COALESCE(v.name, 'Searching Vendor...') AS vendor_name, " +
                "b.problem, " +
                "b.status, " +
                "CASE WHEN ? = 'second_payment' THEN 0.00 ELSE IFNULL(b.deposit, 0.00) END AS deposit, " +
                "IFNULL(b.travelfee, 0.00) AS travelfee, " +
                "CASE WHEN ? = 'second_payment' THEN IFNULL(p.final_amount, 0.00) ELSE IFNULL(p.final_amount, IFNULL(b.totalamount, 0.00)) END AS totalamount, " +
                "CASE WHEN IFNULL(p.payment_paid, 0.00) > 0 THEN IFNULL(p.payment_paid, 0.00) ELSE IFNULL(p.final_amount, 0.00) END AS paymentpaid, " +
                "IFNULL(p.payment_method, '') AS paymentmethod, " +
                "IFNULL(COALESCE(p.paid_at, p.failed_at, p.cancelled_at, p.refunded_at, p.created_at), '') AS paymentdate, " +
                "IFNULL(COALESCE(NULLIF(p.payment_reference, ''), NULLIF(p.gateway_bill_code, ''), NULLIF(p.transaction_id, ''), NULLIF(p.order_id, ''), ''), '') AS payment_reference, " +
                "IFNULL(b.distancekm, 0.00) AS distancekm " +
                "FROM booking b " +
                "LEFT JOIN service s ON b.service_id = s.id " +
                "LEFT JOIN vendor v ON s.vendor_id = v.id " +
                "LEFT JOIN payment p ON p.id = (" +
                "SELECT p1.id FROM payment p1 " +
                "WHERE p1.booking_id = b.id " +
                "AND p1.customer_id = b.customer_id " +
                "AND LOWER(TRIM(p1.payment_type)) = ? " +
                "AND (" +
                "? = '' " +
                "OR p1.payment_reference = ? " +
                "OR p1.order_id = ? " +
                "OR p1.transaction_id = ? " +
                "OR p1.gateway_bill_code = ? " +
                "OR b.id = ?" +
                ") " +
                "ORDER BY CASE " +
                "WHEN LOWER(TRIM(p1.payment_status)) = 'paid' THEN 1 " +
                "WHEN LOWER(TRIM(p1.payment_status)) = 'pending' THEN 2 " +
                "ELSE 3 END, p1.id DESC LIMIT 1" +
                ") " +
                "WHERE b.customer_id = ? " +
                "AND (b.id = ? OR p.id IS NOT NULL) " +
                "LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, cleanPaymentType);
            preparedStatement.setString(2, cleanPaymentType);
            preparedStatement.setString(3, cleanPaymentType);
            preparedStatement.setString(4, searchRef);
            preparedStatement.setString(5, searchRef);
            preparedStatement.setString(6, searchRef);
            preparedStatement.setString(7, searchRef);
            preparedStatement.setString(8, searchRef);
            preparedStatement.setInt(9, cleanId);
            preparedStatement.setInt(10, customerId);
            preparedStatement.setInt(11, cleanId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    ReceiptData receipt = new ReceiptData();

                    receipt.setId(resultSet.getInt("id"));
                    receipt.setBookingdate(resultSet.getString("bookingdate"));
                    receipt.setSubservicebooked(resultSet.getString("service_display"));
                    receipt.setVendor_name(resultSet.getString("vendor_name"));
                    receipt.setProblem(resultSet.getString("problem"));
                    receipt.setStatus(resultSet.getString("status"));
                    receipt.setDeposit(resultSet.getDouble("deposit"));
                    receipt.setTravelfee(resultSet.getDouble("travelfee"));
                    receipt.setTotalamount(resultSet.getDouble("totalamount"));
                    receipt.setPaymentpaid(resultSet.getDouble("paymentpaid"));
                    receipt.setPaymentmethod(resultSet.getString("paymentmethod"));
                    receipt.setPaymentdate(resultSet.getString("paymentdate"));
                    receipt.setPayment_reference(resultSet.getString("payment_reference"));
                    receipt.setDistancekm(resultSet.getDouble("distancekm"));

                    return receipt;
                }
            }
        }

        return null;
    }

    private String normalizePaymentType(String type) {
        String clean = type == null ? "" : type.trim().toLowerCase().replace("-", "_").replace(" ", "_");

        if ("second_payment".equals(clean) || "balance".equals(clean)) {
            return "second_payment";
        }

        return "first_payment";
    }

    private int extractBookingId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        String clean = value.trim();

        if (!clean.matches("\\d+")) {
            return 0;
        }

        try {
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}