package DAO;

import Config.ConnectionManager;
import Model.VendorSearchResult;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchDAO {

    public SearchDAO() {
    }

    private Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null.");
        }

        return connection;
    }

    public List<VendorSearchResult> searchNearbyVendors(String serviceName, String dateInput, double radiusInput, double userLat, double userLng) {
        List<VendorSearchResult> results = new ArrayList<>();

        String sql =
                "SELECT " +
                "s.id AS service_id, " +
                "IFNULL(s.subservice, '') AS subservice, " +
                "IFNULL(CAST(s.avail_time AS CHAR), '') AS avail_time, " +
                "IFNULL(CAST(s.avail_date AS CHAR), '') AS avail_date, " +
                "v.name AS vendor_name, " +
                "IFNULL(v.state, '') AS vendor_state, " +
                "(6371 * ACOS(LEAST(1, GREATEST(-1, " +
                "COS(RADIANS(?)) * COS(RADIANS(CAST(v.latitude AS DECIMAL(12,8)))) * " +
                "COS(RADIANS(CAST(v.longitude AS DECIMAL(12,8))) - RADIANS(?)) + " +
                "SIN(RADIANS(?)) * SIN(RADIANS(CAST(v.latitude AS DECIMAL(12,8))))" +
                ")))) AS distance " +
                "FROM service s " +
                "JOIN vendor v ON s.vendor_id = v.id " +
                "WHERE LOWER(TRIM(s.servicename)) = LOWER(TRIM(?)) " +
                "AND LOWER(TRIM(v.status)) = 'active' " +
                "AND v.latitude IS NOT NULL " +
                "AND v.longitude IS NOT NULL " +
                "AND TRIM(CAST(v.latitude AS CHAR)) <> '' " +
                "AND TRIM(CAST(v.longitude AS CHAR)) <> '' " +
                "AND CAST(v.latitude AS DECIMAL(12,8)) BETWEEN -90 AND 90 " +
                "AND CAST(v.longitude AS DECIMAL(12,8)) BETWEEN -180 AND 180 " +
                "HAVING distance <= ? " +
                "ORDER BY distance ASC";

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
                    String vendorName = safe(rs.getString("vendor_name"));
                    String subservice = safe(rs.getString("subservice"));
                    String vendorState = safe(rs.getString("vendor_state"));
                    String availTimeRange = safe(rs.getString("avail_time"));
                    String availDateRange = safe(rs.getString("avail_date"));

                    if (!isDateAvailable(availDateRange, dateInput)) {
                        continue;
                    }

                    double distance = Math.round(rs.getDouble("distance") * 100.0) / 100.0;

                    List<Integer> bookedHours = getBookedHours(conn, serviceId, dateInput);
                    String availableSlots = filterTimeSlots(availTimeRange, bookedHours);

                    if (!availableSlots.equalsIgnoreCase("Fully Booked") && !availableSlots.trim().isEmpty()) {
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

        String bookSql =
                "SELECT bookingdate " +
                "FROM booking " +
                "WHERE service_id = ? " +
                "AND DATE(bookingdate) = ? " +
                "AND LOWER(TRIM(IFNULL(status, ''))) NOT IN ('rejected', 'reject', 'cancelled', 'payment failed', 'failed')";

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

    private boolean isDateAvailable(String availDateRange, String dateInput) {
        String cleanAvailDate = safe(availDateRange);

        if (cleanAvailDate.isEmpty()) {
            return true;
        }

        String selectedDay = getDayName(dateInput);

        if (selectedDay.isEmpty()) {
            return true;
        }

        String cleanLower = cleanAvailDate.toLowerCase();

        if (cleanLower.contains(dateInput.toLowerCase())) {
            return true;
        }

        if (cleanLower.contains("everyday") || cleanLower.contains("every day") || cleanLower.contains("daily")) {
            return true;
        }

        if (cleanLower.contains(selectedDay.toLowerCase())) {
            return true;
        }

        if (cleanAvailDate.contains("-")) {
            return isWithinDayRange(cleanAvailDate, selectedDay);
        }

        if (cleanAvailDate.contains(",")) {
            String[] days = cleanAvailDate.split(",");

            for (String day : days) {
                if (normalizeDay(day).equals(normalizeDay(selectedDay))) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isWithinDayRange(String availDateRange, String selectedDay) {
        String[] parts = availDateRange.split("-");

        if (parts.length < 2) {
            return false;
        }

        int startDay = dayToNumber(normalizeDay(parts[0]));
        int endDay = dayToNumber(normalizeDay(parts[1]));
        int selected = dayToNumber(normalizeDay(selectedDay));

        if (startDay <= 0 || endDay <= 0 || selected <= 0) {
            return false;
        }

        if (startDay <= endDay) {
            return selected >= startDay && selected <= endDay;
        }

        return selected >= startDay || selected <= endDay;
    }

    private String getDayName(String dateInput) {
        try {
            LocalDate date = LocalDate.parse(dateInput);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            return dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizeDay(String value) {
        String clean = safe(value).toLowerCase();

        clean = clean.replaceAll("[^a-z]", "");

        if (clean.startsWith("mon")) {
            return "monday";
        }

        if (clean.startsWith("tue")) {
            return "tuesday";
        }

        if (clean.startsWith("wed")) {
            return "wednesday";
        }

        if (clean.startsWith("thu")) {
            return "thursday";
        }

        if (clean.startsWith("fri")) {
            return "friday";
        }

        if (clean.startsWith("sat")) {
            return "saturday";
        }

        if (clean.startsWith("sun")) {
            return "sunday";
        }

        return clean;
    }

    private int dayToNumber(String day) {
        if ("monday".equals(day)) {
            return 1;
        }

        if ("tuesday".equals(day)) {
            return 2;
        }

        if ("wednesday".equals(day)) {
            return 3;
        }

        if ("thursday".equals(day)) {
            return 4;
        }

        if ("friday".equals(day)) {
            return 5;
        }

        if ("saturday".equals(day)) {
            return 6;
        }

        if ("sunday".equals(day)) {
            return 7;
        }

        return 0;
    }

    private String filterTimeSlots(String range, List<Integer> bookedHours) {
        String cleanRange = safe(range);

        if (cleanRange.isEmpty()) {
            return "";
        }

        if (!cleanRange.contains("-")) {
            return cleanRange;
        }

        try {
            String[] parts = cleanRange.split("-");
            int startHour = extractHour(parts[0]);
            int endHour = extractHour(parts[1]);

            if (startHour < 0 || endHour < 0 || endHour <= startHour) {
                return cleanRange;
            }

            List<String> freeSlots = new ArrayList<>();

            for (int h = startHour; h < endHour; h++) {
                if (!bookedHours.contains(h)) {
                    freeSlots.add(String.format("%02d:00", h));
                }
            }

            if (freeSlots.isEmpty()) {
                return "Fully Booked";
            }

            return String.join(", ", freeSlots);

        } catch (Exception e) {
            return cleanRange;
        }
    }

    private int extractHour(String value) {
        String clean = safe(value).toUpperCase();

        if (clean.isEmpty()) {
            return -1;
        }

        boolean hasPm = clean.contains("PM");
        boolean hasAm = clean.contains("AM");

        clean = clean.replace("AM", "").replace("PM", "").trim();

        String[] timeParts = clean.split(":");
        int hour = Integer.parseInt(timeParts[0].replaceAll("[^0-9]", ""));

        if (hasPm && hour < 12) {
            hour += 12;
        }

        if (hasAm && hour == 12) {
            hour = 0;
        }

        return hour;
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}