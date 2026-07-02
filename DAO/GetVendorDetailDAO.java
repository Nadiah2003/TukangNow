package DAO;

import Config.ConnectionManager;
import Model.VendorServiceDetail;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GetVendorDetailDAO {

    protected Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public VendorServiceDetail getVendorDetail(int vendorId) throws SQLException {
        VendorServiceDetail detail = null;

        String sql = "SELECT " +
                "v.id, " +
                "v.name, " +
                "v.profile_path, " +
                "s.servicename, " +
                "s.subservice, " +
                "s.startprice, " +
                "s.avail_date, " +
                "s.avail_time, " +
                "IFNULL(ROUND(AVG(r.rating_val), 1), 0.0) AS average_rating " +
                "FROM vendor v " +
                "JOIN service s ON v.id = s.vendor_id " +
                "LEFT JOIN booking b ON s.id = b.service_id " +
                "LEFT JOIN rating r ON b.id = r.booking_id " +
                "WHERE v.id = ? " +
                "GROUP BY v.id, v.name, v.profile_path, s.servicename, s.subservice, s.startprice, s.avail_date, s.avail_time " +
                "LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    detail = new VendorServiceDetail();

                    detail.setId(resultSet.getInt("id"));
                    detail.setName(resultSet.getString("name"));
                    detail.setImg(resultSet.getString("profile_path"));
                    detail.setServiceName(resultSet.getString("servicename"));
                    detail.setSubservice(resultSet.getString("subservice"));
                    detail.setStartprice(resultSet.getString("startprice"));
                    detail.setAvailDate(resultSet.getString("avail_date"));
                    detail.setAvailTime(resultSet.getString("avail_time"));
                    detail.setRating(String.format("%.1f", resultSet.getDouble("average_rating")));
                }
            }
        }

        return detail;
    }
}