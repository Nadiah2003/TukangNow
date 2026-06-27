package DAO;

import Config.DB_TukangNow;
import Model.BookingPageData;
import Model.EstimateItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

    protected Connection getConnection() throws SQLException {
        Connection connection = DB_TukangNow.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public BookingPageData getBookingPageData(int customerId, int vendorId, String selectedDate) throws SQLException {
        BookingPageData data = null;

        String vendorSql = "SELECT " +
                "v.name, " +
                "v.profile_path, " +
                "v.latitude AS vendor_latitude, " +
                "v.longitude AS vendor_longitude, " +
                "s.id AS service_id, " +
                "s.servicename, " +
                "s.subservice, " +
                "s.startprice, " +
                "s.avail_time, " +
                "(6371 * ACOS(LEAST(1, GREATEST(-1, " +
                "COS(RADIANS(?)) * COS(RADIANS(v.latitude)) * COS(RADIANS(v.longitude) - RADIANS(?)) + " +
                "SIN(RADIANS(?)) * SIN(RADIANS(v.latitude)) " +
                ")))) AS distance_km " +
                "FROM vendor v " +
                "JOIN service s ON v.id = s.vendor_id " +
                "WHERE v.id = ? " +
                "LIMIT 1";

        String customerSql = "SELECT name, email, nophone, address, postcode, city, state, latitude, longitude FROM customer WHERE id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement customerStmt = connection.prepareStatement(customerSql)) {

            customerStmt.setInt(1, customerId);

            try (ResultSet customerRs = customerStmt.executeQuery()) {
                if (!customerRs.next()) {
                    throw new SQLException("Customer not found.");
                }

                double customerLatitude = customerRs.getDouble("latitude");
                double customerLongitude = customerRs.getDouble("longitude");

                try (PreparedStatement vendorStmt = connection.prepareStatement(vendorSql)) {
                    vendorStmt.setDouble(1, customerLatitude);
                    vendorStmt.setDouble(2, customerLongitude);
                    vendorStmt.setDouble(3, customerLatitude);
                    vendorStmt.setInt(4, vendorId);

                    try (ResultSet vendorRs = vendorStmt.executeQuery()) {
                        if (!vendorRs.next()) {
                            return null;
                        }

                        data = new BookingPageData();

                        double startPrice = vendorRs.getDouble("startprice");
                        double distance = vendorRs.getDouble("distance_km");

                        if (distance <= 0) {
                            distance = 5.0;
                        }

                        double travelFee = Math.round((distance * 2.50) * 100.0) / 100.0;
                        double totalDeposit = startPrice + travelFee;
                        double[] ratingData = getVendorRating(connection, vendorId);

                        String customerAddress = safe(customerRs.getString("address")) + ", " +
                                safe(customerRs.getString("postcode")) + " " +
                                safe(customerRs.getString("city")) + ", " +
                                safe(customerRs.getString("state"));

                        data.setVendorId(vendorId);
                        data.setVendorName(vendorRs.getString("name"));
                        data.setProfilePic(emptyToDefault(vendorRs.getString("profile_path")));
                        data.setServiceId(vendorRs.getInt("service_id"));
                        data.setServiceName(vendorRs.getString("servicename"));
                        data.setSubServices(vendorRs.getString("subservice"));
                        data.setAvailTime(vendorRs.getString("avail_time"));
                        data.setStartPrice(startPrice);
                        data.setCustName(customerRs.getString("name"));
                        data.setCustEmail(customerRs.getString("email"));
                        data.setCustPhone(customerRs.getString("nophone"));
                        data.setCustAddress(customerAddress);
                        data.setDistance(String.format("%.2f", distance));
                        data.setTravelFee(travelFee);
                        data.setTotalDeposit(totalDeposit);
                        data.setAverageRating(ratingData[0]);
                        data.setRatingCount((int) ratingData[1]);
                        data.setBookedTimes(getBookedTimes(connection, selectedDate, vendorRs.getInt("service_id")));
                    }
                }
            }
        }

        return data;
    }

    public List<EstimateItem> getEstimateItems(String selectedService, int vendorId) throws SQLException {
        String serviceName = safe(selectedService);

        try (Connection connection = getConnection()) {
            List<EstimateItem> items = getEstimateItemsByKeyword(connection, serviceName);

            if (!items.isEmpty()) {
                return items;
            }

            items = searchEstimateItemsByKeyword(connection, serviceName);

            if (!items.isEmpty()) {
                return items;
            }

            String category = getVendorServiceCategory(connection, vendorId).toLowerCase();

            if (category.contains("electrical") || category.contains("electric")) {
                items = getEstimateItemsByKeyword(connection, "default_electrical");
            } else if (category.contains("plumber") || category.contains("plumbing")) {
                items = getEstimateItemsByKeyword(connection, "default_plumber");
            } else if (category.contains("lawn") || category.contains("grass") || category.contains("garden")) {
                items = getEstimateItemsByKeyword(connection, "default_lawn");
            }

            if (!items.isEmpty()) {
                return items;
            }

            return getEstimateItemsByKeyword(connection, "default_general");
        }
    }

    private List<EstimateItem> getEstimateItemsByKeyword(Connection connection, String keyword) throws SQLException {
        List<EstimateItem> items = new ArrayList<>();

        String sql = "SELECT id, service_keyword, item_name, item_price " +
                "FROM estimate_item " +
                "WHERE LOWER(service_keyword) = LOWER(?) " +
                "ORDER BY id ASC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, keyword);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    EstimateItem item = new EstimateItem();
                    item.setId(resultSet.getInt("id"));
                    item.setServiceKeyword(resultSet.getString("service_keyword"));
                    item.setItemName(resultSet.getString("item_name"));
                    item.setItemPrice(resultSet.getDouble("item_price"));
                    items.add(item);
                }
            }
        }

        return items;
    }

    private List<EstimateItem> searchEstimateItemsByKeyword(Connection connection, String selectedService) throws SQLException {
        List<EstimateItem> items = new ArrayList<>();

        String sql = "SELECT id, service_keyword, item_name, item_price " +
                "FROM estimate_item " +
                "WHERE LOWER(?) LIKE CONCAT('%', LOWER(service_keyword), '%') " +
                "AND LOWER(service_keyword) NOT LIKE 'default_%' " +
                "ORDER BY LENGTH(service_keyword) DESC, id ASC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, selectedService);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                String matchedKeyword = "";

                while (resultSet.next()) {
                    String currentKeyword = resultSet.getString("service_keyword");

                    if (matchedKeyword.isEmpty()) {
                        matchedKeyword = currentKeyword;
                    }

                    if (!currentKeyword.equalsIgnoreCase(matchedKeyword)) {
                        continue;
                    }

                    EstimateItem item = new EstimateItem();
                    item.setId(resultSet.getInt("id"));
                    item.setServiceKeyword(currentKeyword);
                    item.setItemName(resultSet.getString("item_name"));
                    item.setItemPrice(resultSet.getDouble("item_price"));
                    items.add(item);
                }
            }
        }

        return items;
    }

    private String getVendorServiceCategory(Connection connection, int vendorId) throws SQLException {
        String sql = "SELECT servicename FROM service WHERE vendor_id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return safe(resultSet.getString("servicename"));
                }
            }
        }

        return "";
    }

    private double[] getVendorRating(Connection connection, int vendorId) throws SQLException {
        double[] ratingData = new double[]{0.0, 0.0};

        String sql = "SELECT " +
                "IFNULL(ROUND(AVG(r.rating_val), 1), 0.0) AS average_rating, " +
                "COUNT(r.rating_val) AS rating_count " +
                "FROM service s " +
                "LEFT JOIN booking b ON s.id = b.service_id " +
                "LEFT JOIN rating r ON b.id = r.booking_id " +
                "WHERE s.vendor_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    ratingData[0] = resultSet.getDouble("average_rating");
                    ratingData[1] = resultSet.getInt("rating_count");
                }
            }
        }

        return ratingData;
    }

    private String[] getBookedTimes(Connection connection, String selectedDate, int serviceId) throws SQLException {
        if (selectedDate == null || selectedDate.trim().isEmpty()) {
            return new String[0];
        }

        String sql = "SELECT TIME(bookingdate) AS booked_time " +
                "FROM booking " +
                "WHERE DATE(bookingdate) = ? " +
                "AND service_id = ? " +
                "AND LOWER(TRIM(status)) IN ('accepted', 'pending', 'paid', 'emergency', 'completed')";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            preparedStatement.setString(1, selectedDate);
            preparedStatement.setInt(2, serviceId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                resultSet.last();
                int size = resultSet.getRow();
                resultSet.beforeFirst();

                String[] bookedTimes = new String[size];
                int index = 0;

                while (resultSet.next()) {
                    bookedTimes[index] = resultSet.getString("booked_time");
                    index++;
                }

                return bookedTimes;
            }
        }
    }

    public int createBooking(int customerId, int vendorId, String subserviceBooked, String date, String time, double deposit, String problem, double travelFee, double distanceKm, String paymentStatus, String paymentReference, String evidencePath) throws SQLException {
        int serviceId = getServiceId(vendorId);
        String bookingDateTime = date + " " + time;
        double totalAmount = deposit;
        double materialCost = 0.00;
        String receiptImg = "";
        double totalBalance = 0.00;

        String sql = "INSERT INTO booking " +
                "(deposit, totalamount, bookingdate, service_id, subservicebooked, problem, status, customer_id, travelfee, materialcost, receiptimg, totalbalance, distancekm, evidencepath, payment_reference) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setDouble(1, deposit);
            preparedStatement.setDouble(2, totalAmount);
            preparedStatement.setString(3, bookingDateTime);
            preparedStatement.setInt(4, serviceId);
            preparedStatement.setString(5, safe(subserviceBooked));
            preparedStatement.setString(6, safe(problem));
            preparedStatement.setString(7, safe(paymentStatus));
            preparedStatement.setInt(8, customerId);
            preparedStatement.setDouble(9, travelFee);
            preparedStatement.setDouble(10, materialCost);
            preparedStatement.setString(11, receiptImg);
            preparedStatement.setDouble(12, totalBalance);
            preparedStatement.setDouble(13, distanceKm);
            preparedStatement.setString(14, safe(evidencePath));
            preparedStatement.setString(15, safe(paymentReference));

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Failed to create booking.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Failed to get booking ID.");
    }

    private int getServiceId(int vendorId) throws SQLException {
        String sql = "SELECT id FROM service WHERE vendor_id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }

        throw new SQLException("Service not found.");
    }

    private String emptyToDefault(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "image/profile.png";
        }

        return value.trim();
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}