package DAO;

import Model.VendorSearchResult;
import Config.DB_TukangNow;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SearchDAO {

    public SearchDAO() {}

    private Connection getConnection() throws SQLException {
        return DB_TukangNow.getConnection();
    }

    public List<VendorSearchResult> searchNearbyVendors(String serviceName, String dateInput, double radiusInput, double userLat, double userLng) {
        List<VendorSearchResult> results = new ArrayList<>();
        
        String sql = "SELECT s.id AS service_id, s.subservice, s.avail_time, "
                   + "v.name AS vendor_name, v.state AS vendor_state, "
                   + "( 6371 * acos( cos( radians(?) ) * cos( radians( v.latitude ) ) * cos( radians( v.longitude ) - radians(?) ) + sin( radians(?) ) * sin( radians( v.latitude ) ) ) ) AS distance "
                   + "FROM service s "
                   + "JOIN vendor v ON s.vendor_id = v.id "
                   + "WHERE s.servicename = ? "
                   + "AND v.status = 'active' "
                   + "HAVING distance <= ? "
                   + "ORDER BY distance ASC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDouble(1, userLat);
            stmt.setDouble(2, userLng);
            stmt.setDouble(3, userLat);
            stmt.setString(4, serviceName);
            stmt.setDouble(5, radiusInput);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int serviceId = rs.getInt("service_id");
                    String vendorName = rs.getString("vendor_name");
                    String subservice = rs.getString("subservice");
                    String vendorState = rs.getString("vendor_state");
                    String availTimeRange = rs.getString("avail_time");
                    
                    double distance = Math.round(rs.getDouble("distance") * 100.0) / 100.0;

                    List<Integer> bookedHours = getBookedHours(conn, serviceId, dateInput);
                    String availableSlots = filterTimeSlots(availTimeRange, bookedHours);

                    if (!availableSlots.equals("Fully Booked") && !availableSlots.isEmpty()) {
                        results.add(new VendorSearchResult(vendorName, distance, subservice, vendorState, availableSlots));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    private List<Integer> getBookedHours(Connection conn, int serviceId, String dateInput) throws SQLException {
        List<Integer> bookedHours = new ArrayList<>();
        String bookSql = "SELECT bookingdate FROM booking WHERE service_id = ? AND DATE(bookingdate) = ?";
        
        try (PreparedStatement stmtBook = conn.prepareStatement(bookSql)) {
            stmtBook.setInt(1, serviceId);
            stmtBook.setString(2, dateInput);
            
            try (ResultSet rsBook = stmtBook.executeQuery()) {
                while (rsBook.next()) {
                    Timestamp timestamp = rsBook.getTimestamp("bookingdate");
                    if (timestamp != null) {
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTime(timestamp);
                        bookedHours.add(cal.get(java.util.Calendar.HOUR_OF_DAY));
                    }
                }
            }
        }
        return bookedHours;
    }

    private String filterTimeSlots(String range, List<Integer> bookedHours) {
        if (range == null || !range.contains("-")) return "";

        try {
            String[] parts = range.split("-");
            int startHour = Integer.parseInt(parts[0].split(":")[0]);
            int endHour = Integer.parseInt(parts[1].split(":")[0]);

            List<String> freeSlots = new ArrayList<>();

            for (int h = startHour; h < endHour; h++) {
                if (!bookedHours.contains(h)) {
                    freeSlots.add(String.format("%02d:00", h));
                }
            }

            if (freeSlots.isEmpty()) return "Fully Booked";
            return String.join(", ", freeSlots);

        } catch (Exception e) {
            return range;
        }
    }
}