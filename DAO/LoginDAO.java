package DAO;

import Config.DB_TukangNow;
import Model.UserSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginDAO {

    public UserSession authenticateUser(String email, String password) {
        UserSession sessionData = new UserSession();

        try (Connection conn = DB_TukangNow.getConnection()) {
            if (conn == null) {
                sessionData.setStatus("error");
                sessionData.setMessage("Database connection is null. Please check DB_TukangNow.");
                return sessionData;
            }

            UserSession adminSession = checkAdmin(conn, email, password);
            if ("success".equals(adminSession.getStatus())) {
                return adminSession;
            }

            UserSession vendorSession = checkVendor(conn, email, password);
            if ("success".equals(vendorSession.getStatus()) || "error".equals(vendorSession.getStatus())) {
                if (vendorSession.getMessage() != null && vendorSession.getMessage().contains("pending approval")) {
                    return vendorSession;
                }

                if ("success".equals(vendorSession.getStatus())) {
                    return vendorSession;
                }
            }

            UserSession customerSession = checkCustomer(conn, email, password);
            if ("success".equals(customerSession.getStatus())) {
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

    private UserSession checkAdmin(Connection conn, String email, String password) throws SQLException {
        UserSession sessionData = new UserSession();

        String sql = "SELECT id, name, role, admin_level FROM admin WHERE email = ? AND password = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
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

        String sql = "SELECT id, name, status, state FROM vendor WHERE email = ? AND password = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");

                    if (status == null || !status.equalsIgnoreCase("active")) {
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

        String sql = "SELECT id, name, state FROM customer WHERE email = ? AND password = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
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
}