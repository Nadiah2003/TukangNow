package DAO;

import Config.ConnectionManager;
import Model.Order;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    private Connection getConnection() throws Exception {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new Exception("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public List<Order> getFilteredOrders(int custId, String status, String service, String date) throws Exception {
        List<Order> orders = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT " +
                "b.id, " +
                "CASE " +
                "WHEN s.servicename IS NOT NULL AND TRIM(s.servicename) <> '' THEN s.servicename " +
                "WHEN b.subservicebooked IS NOT NULL AND TRIM(b.subservicebooked) <> '' THEN b.subservicebooked " +
                "ELSE 'Service' END AS servicename, " +
                "IFNULL(b.problem, '') AS problem, " +
                "IFNULL(DATE_FORMAT(b.bookingdate, '%Y-%m-%d %H:%i'), '') AS bookingdate_display, " +
                "IFNULL(DATE_FORMAT(b.bookingdate, '%Y-%m-%d'), '') AS booking_date_only, " +
                "IFNULL(DATE_FORMAT(b.bookingdate, '%H:%i'), '') AS booking_time_only, " +
                "IFNULL(b.status, '') AS status, " +
                "IFNULL(b.deposit, 0) AS deposit " +
                "FROM booking b " +
                "LEFT JOIN service s ON s.id = b.service_id " +
                "WHERE b.customer_id = ? "
        );

        List<Object> params = new ArrayList<>();
        params.add(custId);

        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND LOWER(TRIM(b.status)) = LOWER(TRIM(?)) ");
            params.add(status.trim());
        }

        if (service != null && !service.trim().isEmpty()) {
            sql.append("AND (LOWER(TRIM(IFNULL(s.servicename, ''))) LIKE ? OR LOWER(TRIM(IFNULL(b.subservicebooked, ''))) LIKE ?) ");
            String keyword = "%" + service.trim().toLowerCase() + "%";
            params.add(keyword);
            params.add(keyword);
        }

        if (date != null && !date.trim().isEmpty()) {
            sql.append("AND DATE(b.bookingdate) = ? ");
            params.add(date.trim());
        }

        sql.append(
                "ORDER BY " +
                "FIELD(b.status, 'Emergency', 'Pending', 'Accepted', 'On the way', 'Arrived', 'Started', 'Second Payment', 'Completed', 'Rated', 'Report', 'Rejected', 'Reject', 'Payment Failed', 'Cancelled'), " +
                "b.bookingdate DESC, b.id DESC"
        );

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Order order = new Order();

                    String bookingDateTime = rs.getString("bookingdate_display");
                    String bookingDateOnly = rs.getString("booking_date_only");
                    String bookingTimeOnly = rs.getString("booking_time_only");

                    order.setId(rs.getInt("id"));
                    order.setServiceName(rs.getString("servicename"));
                    order.setProblem(rs.getString("problem"));
                    order.setDate(bookingDateTime);
                    order.setBookingdate(bookingDateTime);
                    order.setBookingDate(bookingDateOnly);
                    order.setBookingTime(bookingTimeOnly);
                    order.setStatus(rs.getString("status"));
                    order.setDeposit(rs.getDouble("deposit"));

                    orders.add(order);
                }
            }
        }

        return orders;
    }

    public List<String> getBookingStatusList(int custId) throws Exception {
        List<String> statuses = new ArrayList<>();

        String sql =
                "SELECT DISTINCT status " +
                "FROM booking " +
                "WHERE customer_id = ? " +
                "AND status IS NOT NULL " +
                "AND TRIM(status) <> '' " +
                "ORDER BY FIELD(status, 'Emergency', 'Pending', 'Accepted', 'On the way', 'Arrived', 'Started', 'Second Payment', 'Completed', 'Rated', 'Report', 'Rejected', 'Reject', 'Payment Failed', 'Cancelled'), status";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, custId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    statuses.add(rs.getString("status"));
                }
            }
        }

        return statuses;
    }
}