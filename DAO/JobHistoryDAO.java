package DAO;

import Config.DB_TukangNow;
import Model.JobHistory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class JobHistoryDAO {

    private Connection getConnection() throws SQLException {
        Connection connection = DB_TukangNow.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null.");
        }

        return connection;
    }

    public ArrayList<JobHistory> getCompletedJobHistory(int vendorId) throws SQLException {
        ArrayList<JobHistory> jobHistoryList = new ArrayList<>();

        String sql =
                "SELECT " +
                "b.id AS booking_id, " +
                "IFNULL(DATE_FORMAT(b.bookingdate, '%Y-%m-%d %H:%i'), '') AS booking_date, " +
                "CASE " +
                "WHEN b.subservicebooked IS NOT NULL AND TRIM(b.subservicebooked) <> '' THEN b.subservicebooked " +
                "WHEN s.servicename IS NOT NULL AND TRIM(s.servicename) <> '' THEN s.servicename " +
                "ELSE 'Service' END AS subservice, " +
                "(IFNULL(b.totalamount, 0.00) + IFNULL(bmi.material_total, 0.00)) AS amount, " +
                "IFNULL(c.name, '') AS cust_name, " +
                "IFNULL(c.nophone, '') AS cust_phone, " +
                "IFNULL(c.email, '') AS cust_email, " +
                "IFNULL(c.address, '') AS address, " +
                "IFNULL(c.postcode, '') AS postcode, " +
                "IFNULL(c.city, '') AS city, " +
                "IFNULL(c.state, '') AS state, " +
                "IFNULL(r.rating_val, 0.00) AS rating_val, " +
                "IFNULL(r.`comment`, '') AS rating_comment " +
                "FROM booking b " +
                "INNER JOIN service s ON b.service_id = s.id " +
                "INNER JOIN customer c ON b.customer_id = c.id " +
                "LEFT JOIN rating r ON r.booking_id = b.id " +
                "LEFT JOIN ( " +
                "SELECT booking_id, SUM(IFNULL(price, 0.00)) AS material_total " +
                "FROM booking_material_items " +
                "GROUP BY booking_id " +
                ") bmi ON bmi.booking_id = b.id " +
                "WHERE s.vendor_id = ? " +
                "AND LOWER(TRIM(b.status)) IN ('completed', 'rated') " +
                "ORDER BY b.bookingdate DESC, b.id DESC";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    JobHistory job = new JobHistory();

                    String address = safe(resultSet.getString("address"));
                    String postcode = safe(resultSet.getString("postcode"));
                    String city = safe(resultSet.getString("city"));
                    String state = safe(resultSet.getString("state"));

                    job.setBookingId(resultSet.getInt("booking_id"));
                    job.setBookingDate(safe(resultSet.getString("booking_date")));
                    job.setSubservice(safe(resultSet.getString("subservice")));
                    job.setAmount(resultSet.getDouble("amount"));
                    job.setCustName(safe(resultSet.getString("cust_name")));
                    job.setCustPhone(safe(resultSet.getString("cust_phone")));
                    job.setCustEmail(safe(resultSet.getString("cust_email")));
                    job.setCustAddress(buildFullAddress(address, postcode, city, state));
                    job.setRatingVal(resultSet.getDouble("rating_val"));
                    job.setRatingComment(safe(resultSet.getString("rating_comment")));

                    jobHistoryList.add(job);
                }
            }
        }

        return jobHistoryList;
    }

    private String buildFullAddress(String address, String postcode, String city, String state) {
        String firstPart = safe(address);
        String secondPart = (safe(postcode) + " " + safe(city)).trim();
        String thirdPart = safe(state);

        StringBuilder builder = new StringBuilder();

        if (!firstPart.isEmpty()) {
            builder.append(firstPart);
        }

        if (!secondPart.isEmpty()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }

            builder.append(secondPart);
        }

        if (!thirdPart.isEmpty()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }

            builder.append(thirdPart);
        }

        return builder.toString();
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}