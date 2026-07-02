package DAO;

import Config.ConnectionManager;
import Model.UserSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginDAO {

    public UserSession authenticateUser(String email, String password) {
        UserSession sessionData = new UserSession();

        try (Connection conn = ConnectionManager.getConnection()) {
            if (conn == null) {
                sessionData.setStatus("error");
                sessionData.setMessage("Database connection is null. Please check DB_TukangNow.");
                return sessionData;
            }

            autoUnsuspendExpiredAccounts(conn);

            UserSession adminSession = checkAdmin(conn, email, password);

            if ("success".equals(adminSession.getStatus())) {
                return adminSession;
            }

            UserSession vendorSession = checkVendor(conn, email, password);

            if ("success".equals(vendorSession.getStatus())) {
                return vendorSession;
            }

            if ("error".equals(vendorSession.getStatus()) && vendorSession.getMessage() != null && !vendorSession.getMessage().trim().isEmpty()) {
                return vendorSession;
            }

            UserSession customerSession = checkCustomer(conn, email, password);

            if ("success".equals(customerSession.getStatus())) {
                return customerSession;
            }

            if ("error".equals(customerSession.getStatus()) && customerSession.getMessage() != null && !customerSession.getMessage().trim().isEmpty()) {
                return customerSession;
            }

            sessionData.setStatus("error");
            sessionData.setMessage("Invalid Email or Password");
            return sessionData;

        } catch (SQLException e) {
            sessionData.setStatus("error");
            sessionData.setMessage("Database Error: " + e.getMessage());
            return sessionData;
        }
    }

    private void autoUnsuspendExpiredAccounts(Connection conn) throws SQLException {
        String customerSql = "UPDATE customer "
                + "SET status = 'Active', suspendstartdate = NULL, suspendenddate = NULL, banreason = NULL "
                + "WHERE LOWER(TRIM(status)) = 'suspended' "
                + "AND suspendenddate IS NOT NULL "
                + "AND suspendenddate <= NOW()";

        try (PreparedStatement stmt = conn.prepareStatement(customerSql)) {
            stmt.executeUpdate();
        }

        String vendorSql = "UPDATE vendor "
                + "SET status = 'Active', suspendstartdate = NULL, suspendenddate = NULL, banreason = NULL "
                + "WHERE LOWER(TRIM(status)) = 'suspended' "
                + "AND suspendenddate IS NOT NULL "
                + "AND suspendenddate <= NOW()";

        try (PreparedStatement stmt = conn.prepareStatement(vendorSql)) {
            stmt.executeUpdate();
        }
    }

    private UserSession checkAdmin(Connection conn, String email, String password) throws SQLException {
        UserSession sessionData = new UserSession();

        String sql = "SELECT id, name, role, admin_level, password "
                + "FROM admin "
                + "WHERE email = ? "
                + "LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, safe(email));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");

                    if (!isPasswordMatched(password, storedPassword)) {
                        return sessionData;
                    }

                    sessionData.setStatus("success");
                    sessionData.setUserId(rs.getInt("id"));
                    sessionData.setRole("admin");
                    sessionData.setName(rs.getString("name"));
                    sessionData.setAdminLevel(rs.getInt("admin_level"));
                    sessionData.setState("");
                    sessionData.setMessage("Login successful");
                }
            }
        }

        return sessionData;
    }

    private UserSession checkVendor(Connection conn, String email, String password) throws SQLException {
        UserSession sessionData = new UserSession();

        String sql = "SELECT id, name, status, state, password, suspendenddate, banreason, "
                + "TIMESTAMPDIFF(MINUTE, NOW(), suspendenddate) AS remaining_minutes, "
                + "DATE_FORMAT(suspendenddate, '%Y-%m-%d %H:%i') AS suspend_end_display "
                + "FROM vendor "
                + "WHERE email = ? "
                + "LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, safe(email));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");

                    if (!isPasswordMatched(password, storedPassword)) {
                        return sessionData;
                    }

                    String status = safe(rs.getString("status"));
                    String cleanStatus = status.toLowerCase();

                    if (cleanStatus.equals("suspended")) {
                        sessionData.setStatus("error");
                        sessionData.setMessage(buildSuspendedMessage(rs));
                        return sessionData;
                    }

                    if (cleanStatus.equals("banned")) {
                        sessionData.setStatus("error");
                        sessionData.setMessage(buildBannedMessage(rs));
                        return sessionData;
                    }

                    if (!cleanStatus.equals("active")) {
                        sessionData.setStatus("error");
                        sessionData.setMessage("Your account is still pending approval. Please wait for admin verification.");
                        return sessionData;
                    }

                    sessionData.setStatus("success");
                    sessionData.setUserId(rs.getInt("id"));
                    sessionData.setRole("vendor");
                    sessionData.setName(rs.getString("name"));
                    sessionData.setState(rs.getString("state") != null ? rs.getString("state") : "");
                    sessionData.setMessage("Login successful");
                }
            }
        }

        return sessionData;
    }

    private UserSession checkCustomer(Connection conn, String email, String password) throws SQLException {
        UserSession sessionData = new UserSession();

        String sql = "SELECT id, name, state, status, password, suspendenddate, banreason, "
                + "TIMESTAMPDIFF(MINUTE, NOW(), suspendenddate) AS remaining_minutes, "
                + "DATE_FORMAT(suspendenddate, '%Y-%m-%d %H:%i') AS suspend_end_display "
                + "FROM customer "
                + "WHERE email = ? "
                + "LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, safe(email));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");

                    if (!isPasswordMatched(password, storedPassword)) {
                        return sessionData;
                    }

                    String status = safe(rs.getString("status"));
                    String cleanStatus = status.toLowerCase();

                    if (cleanStatus.equals("suspended")) {
                        sessionData.setStatus("error");
                        sessionData.setMessage(buildSuspendedMessage(rs));
                        return sessionData;
                    }

                    if (cleanStatus.equals("banned")) {
                        sessionData.setStatus("error");
                        sessionData.setMessage(buildBannedMessage(rs));
                        return sessionData;
                    }

                    if (!cleanStatus.isEmpty() && !cleanStatus.equals("active")) {
                        sessionData.setStatus("error");
                        sessionData.setMessage("Your account is not active. Please contact admin.");
                        return sessionData;
                    }

                    sessionData.setStatus("success");
                    sessionData.setUserId(rs.getInt("id"));
                    sessionData.setRole("customer");
                    sessionData.setName(rs.getString("name"));
                    sessionData.setState(rs.getString("state") != null ? rs.getString("state") : "");
                    sessionData.setMessage("Login successful");
                }
            }
        }

        return sessionData;
    }

    private boolean isPasswordMatched(String inputPassword, String storedPassword) {
        if (inputPassword == null || storedPassword == null) {
            return false;
        }

        return inputPassword.equals(storedPassword);
    }

    private String buildSuspendedMessage(ResultSet rs) throws SQLException {
        int remainingMinutes = rs.getInt("remaining_minutes");
        String suspendEndDisplay = safe(rs.getString("suspend_end_display"));
        String reason = safe(rs.getString("banreason"));

        if (remainingMinutes <= 0) {
            return "Your account suspension has ended. Please login again.";
        }

        String duration = formatDuration(remainingMinutes);
        String message = "Your account is suspended for " + duration + ".";

        if (!suspendEndDisplay.isEmpty()) {
            message += " Suspension ends on " + suspendEndDisplay + ".";
        }

        if (!reason.isEmpty()) {
            message += " Reason: " + reason;
        }

        return message;
    }

    private String buildBannedMessage(ResultSet rs) throws SQLException {
        String reason = safe(rs.getString("banreason"));

        if (!reason.isEmpty()) {
            return "Your account has been banned. Reason: " + reason;
        }

        return "Your account has been banned. Please contact admin.";
    }

    private String formatDuration(int totalMinutes) {
        if (totalMinutes <= 0) {
            return "less than 1 minute";
        }

        int days = totalMinutes / 1440;
        int remainingAfterDays = totalMinutes % 1440;
        int hours = remainingAfterDays / 60;
        int minutes = remainingAfterDays % 60;

        StringBuilder duration = new StringBuilder();

        if (days > 0) {
            duration.append(days).append(" day");

            if (days > 1) {
                duration.append("s");
            }
        }

        if (hours > 0) {
            if (duration.length() > 0) {
                duration.append(" ");
            }

            duration.append(hours).append(" hour");

            if (hours > 1) {
                duration.append("s");
            }
        }

        if (minutes > 0) {
            if (duration.length() > 0) {
                duration.append(" ");
            }

            duration.append(minutes).append(" minute");

            if (minutes > 1) {
                duration.append("s");
            }
        }

        if (duration.length() == 0) {
            return "less than 1 minute";
        }

        return duration.toString();
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}