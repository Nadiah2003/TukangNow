package DAO;

import Config.ConnectionManager;
import Model.TrackingMaterialItem;
import Model.TrackingVendorData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TrackingVendorDAO {

    private Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null. Please check DB_TukangNow configuration.");
        }

        return connection;
    }

    public TrackingVendorData getTrackingData(int sessionUserId, String requestedView, int bookingId) throws SQLException {
        String sql = "SELECT " +
                "b.id, b.status AS booking_status, b.deposit, b.totalamount, b.bookingdate, b.service_id, " +
                "b.subservicebooked, b.problem, b.customer_id, b.travelfee, b.materialcost, b.totalbalance, b.distancekm, b.evidencepath, " +
                "s.servicename, s.vendor_id, " +
                "v.name AS vendor_name, v.nophone AS vendor_phone, v.profile_path AS vendor_profile_path, " +
                "COALESCE(tr.latitude, v.latitude) AS live_v_lat, " +
                "COALESCE(tr.longitude, v.longitude) AS live_v_lng, " +
                "c.name AS customer_name, c.nophone AS customer_phone, c.email AS customer_email, c.profile_path AS customer_profile_path, " +
                "c.address AS customer_address, c.postcode AS customer_postcode, c.city AS customer_city, c.state AS customer_state, c.latitude AS c_lat, c.longitude AS c_lng, " +
                "IFNULL(bti.vehicle_model, '') AS vehicle_model, IFNULL(bti.plate_number, '') AS plate_number, " +
                "IFNULL(bti.arrival_evidencepath, '') AS arrival_evidencepath, IFNULL(bti.completion_evidencepath, '') AS completion_evidencepath, " +
                "(SELECT COALESCE(ROUND(AVG(r.rating_val), 1), 0.0) FROM rating r JOIN booking bk ON r.booking_id = bk.id JOIN service sv ON bk.service_id = sv.id WHERE sv.vendor_id = v.id) AS avg_rating, " +
                "rt.rating_val, rt.comment " +
                "FROM booking b " +
                "JOIN service s ON b.service_id = s.id " +
                "JOIN vendor v ON s.vendor_id = v.id " +
                "JOIN customer c ON b.customer_id = c.id " +
                "LEFT JOIN tracking tr ON tr.id = (SELECT tr2.id FROM tracking tr2 WHERE tr2.vendor_id = v.id AND tr2.is_active = 1 ORDER BY tr2.`timestamp` DESC, tr2.id DESC LIMIT 1) " +
                "LEFT JOIN booking_tracking_info bti ON b.id = bti.booking_id " +
                "LEFT JOIN rating rt ON b.id = rt.booking_id " +
                "WHERE b.id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, bookingId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int customerId = resultSet.getInt("customer_id");
                    int vendorId = resultSet.getInt("vendor_id");
                    String viewerRole = resolveViewerRole(sessionUserId, requestedView, customerId, vendorId);

                    if (viewerRole.isEmpty()) {
                        return null;
                    }

                    TrackingVendorData data = new TrackingVendorData();

                    String bookingStatus = safe(resultSet.getString("booking_status"));
                    String subservice = safe(resultSet.getString("subservicebooked"));
                    Timestamp bookingTimestamp = resultSet.getTimestamp("bookingdate");
                    double vendorLat = resultSet.getDouble("live_v_lat");
                    double vendorLng = resultSet.getDouble("live_v_lng");
                    double customerLat = resultSet.getDouble("c_lat");
                    double customerLng = resultSet.getDouble("c_lng");
                    double distanceKm = calculateDistanceKm(vendorLat, vendorLng, customerLat, customerLng);
                    long fallbackSeconds = calculateFallbackSeconds(distanceKm);

                    data.setStatus("success");
                    data.setMessage("Tracking loaded.");
                    data.setViewer_role(viewerRole);
                    data.setBooking_id(resultSet.getInt("id"));
                    data.setService_id(resultSet.getInt("service_id"));
                    data.setCurrent_status(toDisplayStatus(bookingStatus));
                    data.setTracking_status(toDisplayStatus(bookingStatus));
                    data.setBookingdate(safe(resultSet.getString("bookingdate")));
                    data.setBooking_date_reached(bookingTimestamp != null && System.currentTimeMillis() >= bookingTimestamp.getTime());
                    data.setIs_emergency(isEmergencyBooking(subservice));
                    data.setBooking_service(safe(resultSet.getString("servicename")));
                    data.setSubservicebooked(subservice);
                    data.setProblem(safe(resultSet.getString("problem")));
                    data.setDeposit(resultSet.getDouble("deposit"));
                    data.setTotalamount(resultSet.getDouble("totalamount"));
                    data.setTravelfee(resultSet.getDouble("travelfee"));
                    data.setMaterialcost(resultSet.getDouble("materialcost"));
                    data.setTotalbalance(resultSet.getDouble("totalbalance"));
                    data.setEvidencepath(safe(resultSet.getString("evidencepath")));
                    data.setVendor_lat(vendorLat);
                    data.setVendor_lng(vendorLng);
                    data.setCust_lat(customerLat);
                    data.setCust_lng(customerLng);
                    data.setVendor_name(safe(resultSet.getString("vendor_name")));
                    data.setVendor_phone(safe(resultSet.getString("vendor_phone")));
                    data.setProfile_path(safe(resultSet.getString("vendor_profile_path")));
                    data.setCustomer_name(safe(resultSet.getString("customer_name")));
                    data.setCustomer_phone(safe(resultSet.getString("customer_phone")));
                    data.setCustomer_email(safe(resultSet.getString("customer_email")));
                    data.setCustomer_profile_path(safe(resultSet.getString("customer_profile_path")));
                    data.setCustomer_address(buildAddress(resultSet));
                    data.setEta(shouldCalculateEta(bookingStatus) ? buildEta(fallbackSeconds) : buildStaticEta(bookingStatus));
                    data.setRoute_geometry("");
                    data.setVendor_rating(String.format("%.1f", resultSet.getDouble("avg_rating")));
                    data.setDuration_mins(String.valueOf(Math.max(1, Math.round(fallbackSeconds / 60.0))));
                    data.setDistance_km(String.format("%.1f", distanceKm));
                    data.setVehicle_model(safe(resultSet.getString("vehicle_model")));
                    data.setPlate_number(safe(resultSet.getString("plate_number")));
                    data.setArrival_evidencepath(safe(resultSet.getString("arrival_evidencepath")));
                    data.setCompletion_evidencepath(safe(resultSet.getString("completion_evidencepath")));

                    String ratingValue = safe(resultSet.getString("rating_val"));
                    String ratingComment = safe(resultSet.getString("comment"));

                    data.setHas_rated(!ratingValue.isEmpty());
                    data.setRating_val(ratingValue);
                    data.setRating_comment(ratingComment);
                    data.setMaterials(getMaterials(connection, bookingId));

                    return data;
                }
            }
        }

        return null;
    }

    public boolean updateVendorTrackingLocation(int vendorId, int bookingId, double latitude, double longitude) throws SQLException {
        if (!isValidMalaysiaCoordinate(latitude, longitude)) {
            return false;
        }

        try (Connection connection = getConnection()) {
            if (!isVendorOwner(connection, vendorId, bookingId)) {
                return false;
            }

            if (!isBookingStatus(connection, bookingId, "on the way")) {
                return false;
            }

            int activeTrackingId = 0;

            String selectSql = "SELECT id FROM tracking WHERE vendor_id = ? AND is_active = 1 ORDER BY `timestamp` DESC, id DESC LIMIT 1";

            try (PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {
                preparedStatement.setInt(1, vendorId);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        activeTrackingId = resultSet.getInt("id");
                    }
                }
            }

            if (activeTrackingId > 0) {
                String updateSql = "UPDATE tracking SET latitude = ?, longitude = ?, is_active = ?, `timestamp` = NOW() WHERE id = ?";

                try (PreparedStatement preparedStatement = connection.prepareStatement(updateSql)) {
                    preparedStatement.setDouble(1, latitude);
                    preparedStatement.setDouble(2, longitude);
                    preparedStatement.setInt(3, 1);
                    preparedStatement.setInt(4, activeTrackingId);

                    return preparedStatement.executeUpdate() > 0;
                }
            }

            String insertSql = "INSERT INTO tracking (vendor_id, latitude, longitude, is_active, `timestamp`) VALUES (?, ?, ?, ?, NOW())";

            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
                preparedStatement.setInt(1, vendorId);
                preparedStatement.setDouble(2, latitude);
                preparedStatement.setDouble(3, longitude);
                preparedStatement.setInt(4, 1);

                return preparedStatement.executeUpdate() > 0;
            }
        }
    }

    public boolean updateOnTheWay(int vendorId, int bookingId, String vehicleModel, String plateNumber) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (!isVendorOwner(connection, vendorId, bookingId)) {
                    connection.rollback();
                    return false;
                }

                upsertTrackingVehicle(connection, bookingId, vehicleModel, plateNumber);

                String sql = "UPDATE booking SET status = 'On The Way' WHERE id = ? AND LOWER(TRIM(status)) = 'accepted'";

                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setInt(1, bookingId);

                    if (preparedStatement.executeUpdate() <= 0) {
                        connection.rollback();
                        return false;
                    }
                }

                insertInitialTrackingFromVendorCoordinate(connection, vendorId);

                connection.commit();
                return true;

            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean updateArrived(int vendorId, int bookingId) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                String sql = "UPDATE booking b JOIN service s ON b.service_id = s.id " +
                        "SET b.status = 'Arrived' " +
                        "WHERE b.id = ? AND s.vendor_id = ? AND LOWER(TRIM(b.status)) = 'on the way'";

                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setInt(1, bookingId);
                    preparedStatement.setInt(2, vendorId);

                    if (preparedStatement.executeUpdate() <= 0) {
                        connection.rollback();
                        return false;
                    }
                }

                try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE tracking SET is_active = ? WHERE vendor_id = ?")) {
                    preparedStatement.setInt(1, 0);
                    preparedStatement.setInt(2, vendorId);
                    preparedStatement.executeUpdate();
                }

                connection.commit();
                return true;

            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean updateStarted(int vendorId, int bookingId, String arrivalEvidencePath) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (!isVendorOwner(connection, vendorId, bookingId)) {
                    connection.rollback();
                    return false;
                }

                upsertArrivalEvidence(connection, bookingId, arrivalEvidencePath);

                String sql = "UPDATE booking SET status = 'Started' WHERE id = ? AND LOWER(TRIM(status)) = 'arrived'";

                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setInt(1, bookingId);

                    if (preparedStatement.executeUpdate() <= 0) {
                        connection.rollback();
                        return false;
                    }
                }

                connection.commit();
                return true;

            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean completeWork(int vendorId, int bookingId, String completionEvidencePath, double laborCharge, List<TrackingMaterialItem> items) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (!isVendorOwner(connection, vendorId, bookingId)) {
                    connection.rollback();
                    return false;
                }

                if (!isBookingStatus(connection, bookingId, "started")) {
                    connection.rollback();
                    return false;
                }

                upsertCompletionEvidence(connection, bookingId, completionEvidencePath);
                deleteMaterialItems(connection, bookingId);
                deleteMaterialReceipts(connection, bookingId);

                double materialTotal = 0.0;

                if (laborCharge > 0) {
                    TrackingMaterialItem laborItem = new TrackingMaterialItem();
                    laborItem.setBooking_id(bookingId);
                    laborItem.setReceipt_id(0);
                    laborItem.setReceipt_label("");
                    laborItem.setItem_name("Service Labour Charge");
                    laborItem.setQuantity(1);
                    laborItem.setPrice(laborCharge);
                    laborItem.setReceipt_path("");
                    insertMaterialItem(connection, bookingId, laborItem, 0);
                }

                Map<String, Integer> receiptIdMap = new LinkedHashMap<>();

                for (TrackingMaterialItem item : items) {
                    int receiptId = 0;
                    String receiptPath = safe(item.getReceipt_path());

                    if (!receiptPath.isEmpty()) {
                        if (receiptIdMap.containsKey(receiptPath)) {
                            receiptId = receiptIdMap.get(receiptPath);
                        } else {
                            receiptId = insertMaterialReceipt(connection, bookingId, item.getReceipt_label(), receiptPath);
                            receiptIdMap.put(receiptPath, receiptId);
                        }
                    }

                    insertMaterialItem(connection, bookingId, item, receiptId);
                    materialTotal += item.getPrice() * item.getQuantity();
                }

                materialTotal = Math.round(materialTotal * 100.0) / 100.0;
                double balanceAmount = Math.round((materialTotal + laborCharge) * 100.0) / 100.0;

                String updateBookingSql = "UPDATE booking SET materialcost = ?, totalbalance = ?, status = 'Second Payment' WHERE id = ?";

                try (PreparedStatement preparedStatement = connection.prepareStatement(updateBookingSql)) {
                    preparedStatement.setDouble(1, materialTotal);
                    preparedStatement.setDouble(2, balanceAmount);
                    preparedStatement.setInt(3, bookingId);
                    preparedStatement.executeUpdate();
                }

                upsertSecondPayment(connection, bookingId, balanceAmount);

                connection.commit();
                return true;

            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean submitRating(int customerId, int bookingId, int ratingVal, String comment) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (!isCustomerOwner(connection, customerId, bookingId)) {
                    connection.rollback();
                    return false;
                }

                if (!isBookingStatus(connection, bookingId, "completed")) {
                    connection.rollback();
                    return false;
                }

                if (hasRating(connection, bookingId)) {
                    connection.rollback();
                    return false;
                }

                String insertSql = "INSERT INTO rating (booking_id, rating_val, comment) VALUES (?, ?, ?)";

                try (PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
                    preparedStatement.setInt(1, bookingId);
                    preparedStatement.setInt(2, ratingVal);
                    preparedStatement.setString(3, safe(comment));
                    preparedStatement.executeUpdate();
                }

                String updateSql = "UPDATE booking SET status = 'Rated' WHERE id = ?";

                try (PreparedStatement preparedStatement = connection.prepareStatement(updateSql)) {
                    preparedStatement.setInt(1, bookingId);
                    preparedStatement.executeUpdate();
                }

                connection.commit();
                return true;

            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void insertInitialTrackingFromVendorCoordinate(Connection connection, int vendorId) throws SQLException {
        double vendorLatitude = 0.0;
        double vendorLongitude = 0.0;

        String selectVendorSql = "SELECT latitude, longitude FROM vendor WHERE id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(selectVendorSql)) {
            preparedStatement.setInt(1, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    vendorLatitude = resultSet.getDouble("latitude");
                    vendorLongitude = resultSet.getDouble("longitude");
                }
            }
        }

        if (!isValidMalaysiaCoordinate(vendorLatitude, vendorLongitude)) {
            return;
        }

        String deactivateSql = "UPDATE tracking SET is_active = ? WHERE vendor_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(deactivateSql)) {
            preparedStatement.setInt(1, 0);
            preparedStatement.setInt(2, vendorId);
            preparedStatement.executeUpdate();
        }

        String insertSql = "INSERT INTO tracking (vendor_id, latitude, longitude, is_active, `timestamp`) VALUES (?, ?, ?, ?, NOW())";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
            preparedStatement.setInt(1, vendorId);
            preparedStatement.setDouble(2, vendorLatitude);
            preparedStatement.setDouble(3, vendorLongitude);
            preparedStatement.setInt(4, 1);
            preparedStatement.executeUpdate();
        }
    }

    private void upsertTrackingVehicle(Connection connection, int bookingId, String vehicleModel, String plateNumber) throws SQLException {
        String sql = "INSERT INTO booking_tracking_info (booking_id, vehicle_model, plate_number) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE vehicle_model = VALUES(vehicle_model), plate_number = VALUES(plate_number), updated_at = NOW()";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setString(2, safe(vehicleModel));
            preparedStatement.setString(3, safe(plateNumber));
            preparedStatement.executeUpdate();
        }
    }

    private void upsertArrivalEvidence(Connection connection, int bookingId, String arrivalEvidencePath) throws SQLException {
        String sql = "INSERT INTO booking_tracking_info (booking_id, arrival_evidencepath) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE arrival_evidencepath = VALUES(arrival_evidencepath), updated_at = NOW()";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setString(2, safe(arrivalEvidencePath));
            preparedStatement.executeUpdate();
        }
    }

    private void upsertCompletionEvidence(Connection connection, int bookingId, String completionEvidencePath) throws SQLException {
        String sql = "INSERT INTO booking_tracking_info (booking_id, completion_evidencepath) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE completion_evidencepath = VALUES(completion_evidencepath), updated_at = NOW()";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setString(2, safe(completionEvidencePath));
            preparedStatement.executeUpdate();
        }
    }

    private void deleteMaterialItems(Connection connection, int bookingId) throws SQLException {
        String sql = "DELETE FROM booking_material_items WHERE booking_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.executeUpdate();
        }
    }

    private void deleteMaterialReceipts(Connection connection, int bookingId) throws SQLException {
        String sql = "DELETE FROM booking_material_receipts WHERE booking_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.executeUpdate();
        }
    }

    private int insertMaterialReceipt(Connection connection, int bookingId, String receiptLabel, String receiptPath) throws SQLException {
        String sql = "INSERT INTO booking_material_receipts (booking_id, receipt_label, receipt_path) VALUES (?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setString(2, safe(receiptLabel));
            preparedStatement.setString(3, safe(receiptPath));
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        return 0;
    }

    private void insertMaterialItem(Connection connection, int bookingId, TrackingMaterialItem item, int receiptId) throws SQLException {
        String sql = "INSERT INTO booking_material_items (booking_id, receipt_id, item_name, quantity, price, receipt_path) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);

            if (receiptId > 0) {
                preparedStatement.setInt(2, receiptId);
            } else {
                preparedStatement.setNull(2, java.sql.Types.INTEGER);
            }

            preparedStatement.setString(3, safe(item.getItem_name()));
            preparedStatement.setInt(4, item.getQuantity());
            preparedStatement.setDouble(5, item.getPrice());
            preparedStatement.setString(6, safe(item.getReceipt_path()));
            preparedStatement.executeUpdate();
        }
    }

    private void upsertSecondPayment(Connection connection, int bookingId, double balanceAmount) throws SQLException {
        int customerId = 0;

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT customer_id FROM booking WHERE id = ? LIMIT 1")) {
            preparedStatement.setInt(1, bookingId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    customerId = resultSet.getInt("customer_id");
                }
            }
        }

        if (customerId <= 0) {
            throw new SQLException("Customer ID not found for second payment.");
        }

        int paymentId = 0;

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT id FROM payment WHERE booking_id = ? AND payment_type = 'second_payment' LIMIT 1")) {
            preparedStatement.setInt(1, bookingId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    paymentId = resultSet.getInt("id");
                }
            }
        }

        if (paymentId > 0) {
            String updateSql = "UPDATE payment SET amount_original = ?, final_amount = ?, payment_paid = 0.00, payment_status = 'pending', payment_method = 'balance', gateway_name = 'TukangNow Balance', gateway_response = NULL, failed_reason = NULL, updated_at = NOW() WHERE id = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(updateSql)) {
                preparedStatement.setDouble(1, balanceAmount);
                preparedStatement.setDouble(2, balanceAmount);
                preparedStatement.setInt(3, paymentId);
                preparedStatement.executeUpdate();
            }

            return;
        }

        String reference = "BAL-" + bookingId + "-" + System.currentTimeMillis();

        String insertSql = "INSERT INTO payment " +
                "(booking_id, customer_id, customer_voucher_id, payment_type, payment_method, payment_status, amount_original, points_discount, voucher_discount, final_amount, payment_paid, reward_points_used, reward_points_earned, payment_reference, order_id, transaction_id, gateway_name, gateway_response, created_at) " +
                "VALUES (?, ?, NULL, 'second_payment', 'balance', 'pending', ?, 0.00, 0.00, ?, 0.00, 0, 0, ?, ?, ?, 'TukangNow Balance', NULL, NOW())";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, customerId);
            preparedStatement.setDouble(3, balanceAmount);
            preparedStatement.setDouble(4, balanceAmount);
            preparedStatement.setString(5, reference);
            preparedStatement.setString(6, reference);
            preparedStatement.setString(7, reference);
            preparedStatement.executeUpdate();
        }
    }

    private List<TrackingMaterialItem> getMaterials(Connection connection, int bookingId) throws SQLException {
        List<TrackingMaterialItem> items = new ArrayList<>();

        String sql = "SELECT " +
                "bmi.id, bmi.booking_id, IFNULL(bmi.receipt_id, 0) AS receipt_id, bmi.item_name, bmi.quantity, bmi.price, " +
                "IFNULL(bmr.receipt_label, '') AS receipt_label, " +
                "IFNULL(bmr.receipt_path, IFNULL(bmi.receipt_path, '')) AS receipt_path " +
                "FROM booking_material_items bmi " +
                "LEFT JOIN booking_material_receipts bmr ON bmi.receipt_id = bmr.id " +
                "WHERE bmi.booking_id = ? " +
                "ORDER BY IFNULL(bmi.receipt_id, 0), bmi.id ASC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    TrackingMaterialItem item = new TrackingMaterialItem();

                    item.setId(resultSet.getInt("id"));
                    item.setBooking_id(resultSet.getInt("booking_id"));
                    item.setReceipt_id(resultSet.getInt("receipt_id"));
                    item.setReceipt_label(safe(resultSet.getString("receipt_label")));
                    item.setItem_name(safe(resultSet.getString("item_name")));
                    item.setQuantity(resultSet.getInt("quantity"));
                    item.setPrice(resultSet.getDouble("price"));
                    item.setReceipt_path(safe(resultSet.getString("receipt_path")));

                    items.add(item);
                }
            }
        }

        return items;
    }

    private boolean isVendorOwner(Connection connection, int vendorId, int bookingId) throws SQLException {
        String sql = "SELECT b.id FROM booking b JOIN service s ON b.service_id = s.id WHERE b.id = ? AND s.vendor_id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, vendorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean isCustomerOwner(Connection connection, int customerId, int bookingId) throws SQLException {
        String sql = "SELECT id FROM booking WHERE id = ? AND customer_id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setInt(2, customerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean isBookingStatus(Connection connection, int bookingId, String status) throws SQLException {
        String sql = "SELECT id FROM booking WHERE id = ? AND LOWER(TRIM(status)) = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.setString(2, status.toLowerCase());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean hasRating(Connection connection, int bookingId) throws SQLException {
        String sql = "SELECT id FROM rating WHERE booking_id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String resolveViewerRole(int sessionUserId, String requestedView, int customerId, int vendorId) {
        String view = safe(requestedView).toLowerCase();

        if ("vendor".equals(view) && sessionUserId == vendorId) {
            return "vendor";
        }

        if ("customer".equals(view) && sessionUserId == customerId) {
            return "customer";
        }

        if (sessionUserId == customerId) {
            return "customer";
        }

        if (sessionUserId == vendorId) {
            return "vendor";
        }

        return "";
    }

    private boolean shouldCalculateEta(String status) {
        String clean = safe(status).toLowerCase();

        return clean.equals("on the way");
    }

    private String buildStaticEta(String status) {
        String clean = safe(status).toLowerCase();

        if (clean.equals("arrived") || clean.equals("started") || clean.equals("second payment") || clean.equals("completed") || clean.equals("rated")) {
            return "Arrived";
        }

        return "--:--";
    }

    private String buildEta(long seconds) {
        try {
            return LocalTime.now().plusSeconds(seconds).format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return "--:--";
        }
    }

    private long calculateFallbackSeconds(double distanceKm) {
        if (distanceKm <= 0) {
            return 600;
        }

        return Math.round((distanceKm / 40.0) * 3600.0) + 120;
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        if (!isValidCoordinate(lat1, lon1) || !isValidCoordinate(lat2, lon2)) {
            return 0.0;
        }

        double earthRadiusKm = 6371.0;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadiusKm * c;

        return Math.round(distance * 10.0) / 10.0;
    }

    private boolean isValidCoordinate(double latitude, double longitude) {
        if (latitude == 0.0 && longitude == 0.0) {
            return false;
        }

        return latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0;
    }

    private boolean isValidMalaysiaCoordinate(double latitude, double longitude) {
        return latitude >= 0.5 && latitude <= 7.8 && longitude >= 99.0 && longitude <= 119.5;
    }

    private String toDisplayStatus(String status) {
        String clean = safe(status).toLowerCase();

        if (clean.equals("accepted")) {
            return "Accepted";
        }

        if (clean.equals("on the way") || clean.equals("on theway") || clean.equals("ontheway")) {
            return "On The Way";
        }

        if (clean.equals("arrived")) {
            return "Arrived";
        }

        if (clean.equals("started") || clean.equals("work started")) {
            return "Started";
        }

        if (clean.equals("second payment") || clean.equals("second_payment")) {
            return "Second Payment";
        }

        if (clean.equals("completed")) {
            return "Completed";
        }

        if (clean.equals("rated")) {
            return "Rated";
        }

        return safe(status);
    }

    private boolean isEmergencyBooking(String subservice) {
        return safe(subservice).equalsIgnoreCase("Emergency");
    }

    private String buildAddress(ResultSet resultSet) throws SQLException {
        String address = safe(resultSet.getString("customer_address"));
        String postcode = safe(resultSet.getString("customer_postcode"));
        String city = safe(resultSet.getString("customer_city"));
        String state = safe(resultSet.getString("customer_state"));
        String lineTwo = (postcode + " " + city).trim();

        StringBuilder builder = new StringBuilder();

        if (!address.isEmpty()) {
            builder.append(address);
        }

        if (!lineTwo.isEmpty()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }

            builder.append(lineTwo);
        }

        if (!state.isEmpty()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }

            builder.append(state);
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