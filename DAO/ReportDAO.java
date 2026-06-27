package DAO;

import Config.DB_TukangNow;
import Model.Report;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ReportDAO {

    private Connection getConnection() throws SQLException {
        Connection connection = DB_TukangNow.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null.");
        }

        return connection;
    }

    public String submitReport(Report report) throws SQLException {
        if (report == null) {
            return "Invalid report.";
        }

        String reporterType = safe(report.getReporterType()).toLowerCase();
        String reportedType = safe(report.getReportedType()).toLowerCase();
        String reportOption = safe(report.getReportOption());
        String explanation = safe(report.getExplanation());

        if (!"customer".equals(reporterType) && !"vendor".equals(reporterType)) {
            return "Invalid reporter account type.";
        }

        if (!"customer".equals(reportedType) && !"vendor".equals(reportedType)) {
            return "Invalid reported account type.";
        }

        if (report.getReporterId() <= 0) {
            return "Session expired.";
        }

        if (report.getReportedId() <= 0) {
            return "Invalid reported account ID.";
        }

        if (reportOption.isEmpty()) {
            return "Please choose a report option.";
        }

        if (explanation.isEmpty()) {
            return "Please write your explanation.";
        }

        if (explanation.length() < 10) {
            return "Explanation must be at least 10 characters.";
        }

        if (explanation.length() > 1000) {
            return "Explanation cannot exceed 1000 characters.";
        }

        try (Connection connection = getConnection()) {
            if (!isValidAccount(connection, reporterType, report.getReporterId())) {
                return "Invalid reporter account.";
            }

            if (!isValidAccount(connection, reportedType, report.getReportedId())) {
                return "Reported account not found.";
            }

            insertReport(connection, reporterType, report.getReporterId(), reportedType, report.getReportedId(), reportOption, explanation);
        }

        return "success";
    }

    public ArrayList<Report> getAllReports() throws SQLException {
        ArrayList<Report> reports = new ArrayList<>();

        String sql = "SELECT " +
                "r.id, r.reporter_type, r.reporter_id, r.reported_type, r.reported_id, " +
                "r.report_option, r.explanation, r.status, r.admin_note, r.action_taken, " +
                "IFNULL(r.action_admin_id, 0) AS action_admin_id, " +
                "IFNULL(DATE_FORMAT(r.action_date, '%Y-%m-%d %H:%i'), '') AS action_date, " +
                "IFNULL(DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i'), '') AS created_at, " +
                "CASE WHEN r.reporter_type = 'customer' THEN IFNULL(rc.name, 'Unknown Customer') ELSE IFNULL(rv.name, 'Unknown Vendor') END AS reporter_name, " +
                "CASE WHEN r.reported_type = 'customer' THEN IFNULL(tc.name, 'Unknown Customer') ELSE IFNULL(tv.name, 'Unknown Vendor') END AS reported_name, " +
                "CASE WHEN r.reported_type = 'customer' THEN IFNULL(tc.status, 'Active') ELSE IFNULL(tv.status, '-') END AS reported_account_status " +
                "FROM reports r " +
                "LEFT JOIN customer rc ON r.reporter_type = 'customer' AND r.reporter_id = rc.id " +
                "LEFT JOIN vendor rv ON r.reporter_type = 'vendor' AND r.reporter_id = rv.id " +
                "LEFT JOIN customer tc ON r.reported_type = 'customer' AND r.reported_id = tc.id " +
                "LEFT JOIN vendor tv ON r.reported_type = 'vendor' AND r.reported_id = tv.id " +
                "ORDER BY r.created_at DESC, r.id DESC";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                Report report = new Report();

                report.setId(resultSet.getInt("id"));
                report.setReporterType(safe(resultSet.getString("reporter_type")));
                report.setReporterId(resultSet.getInt("reporter_id"));
                report.setReporterName(safe(resultSet.getString("reporter_name")));
                report.setReportedType(safe(resultSet.getString("reported_type")));
                report.setReportedId(resultSet.getInt("reported_id"));
                report.setReportedName(safe(resultSet.getString("reported_name")));
                report.setReportedAccountStatus(safe(resultSet.getString("reported_account_status")));
                report.setReportOption(safe(resultSet.getString("report_option")));
                report.setExplanation(safe(resultSet.getString("explanation")));
                report.setStatus(safe(resultSet.getString("status")));
                report.setAdminNote(safe(resultSet.getString("admin_note")));
                report.setActionTaken(safe(resultSet.getString("action_taken")));
                report.setActionAdminId(resultSet.getInt("action_admin_id"));
                report.setActionDate(safe(resultSet.getString("action_date")));
                report.setCreatedAt(safe(resultSet.getString("created_at")));

                reports.add(report);
            }
        }

        return reports;
    }

    public String markInvestigating(int reportId, int adminId, String adminNote) throws SQLException {
        if (reportId <= 0) {
            return "Invalid report ID.";
        }

        String sql = "UPDATE reports " +
                "SET status = 'Investigating', admin_note = ?, action_taken = 'Investigating', action_admin_id = ?, action_date = NOW() " +
                "WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(adminNote));
            preparedStatement.setInt(2, adminId);
            preparedStatement.setInt(3, reportId);

            if (preparedStatement.executeUpdate() > 0) {
                return "success";
            }
        }

        return "Report not found.";
    }

    public String resolveReport(int reportId, int adminId, String adminNote) throws SQLException {
        if (reportId <= 0) {
            return "Invalid report ID.";
        }

        String sql = "UPDATE reports " +
                "SET status = 'Resolved', admin_note = ?, action_taken = 'Resolved', action_admin_id = ?, action_date = NOW() " +
                "WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safe(adminNote));
            preparedStatement.setInt(2, adminId);
            preparedStatement.setInt(3, reportId);

            if (preparedStatement.executeUpdate() > 0) {
                return "success";
            }
        }

        return "Report not found.";
    }

    public String suspendReportedAccount(int reportId, int adminId, int suspendDays, String adminNote) throws SQLException {
        if (reportId <= 0) {
            return "Invalid report ID.";
        }

        if (suspendDays <= 0) {
            return "Suspend days must be more than 0.";
        }

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                Report target = getReportTargetForUpdate(connection, reportId);

                if (target == null) {
                    connection.rollback();
                    return "Report not found.";
                }

                if ("customer".equals(target.getReportedType())) {
                    suspendCustomer(connection, target.getReportedId(), suspendDays, adminNote);
                } else if ("vendor".equals(target.getReportedType())) {
                    suspendVendor(connection, target.getReportedId(), suspendDays, adminNote);
                } else {
                    connection.rollback();
                    return "Invalid reported account type.";
                }

                updateReportAfterAction(connection, reportId, "Action Taken", "Suspended", adminId, adminNote);

                connection.commit();
                return "success";
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public String banReportedAccount(int reportId, int adminId, String adminNote) throws SQLException {
        if (reportId <= 0) {
            return "Invalid report ID.";
        }

        if (safe(adminNote).isEmpty()) {
            return "Ban reason cannot be empty.";
        }

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                Report target = getReportTargetForUpdate(connection, reportId);

                if (target == null) {
                    connection.rollback();
                    return "Report not found.";
                }

                if ("customer".equals(target.getReportedType())) {
                    banCustomer(connection, target.getReportedId(), adminNote);
                } else if ("vendor".equals(target.getReportedType())) {
                    banVendor(connection, target.getReportedId(), adminNote);
                } else {
                    connection.rollback();
                    return "Invalid reported account type.";
                }

                updateReportAfterAction(connection, reportId, "Action Taken", "Banned", adminId, adminNote);

                connection.commit();
                return "success";
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private boolean isValidAccount(Connection connection, String accountType, int accountId) throws SQLException {
        String tableName;

        if ("customer".equals(accountType)) {
            tableName = "customer";
        } else if ("vendor".equals(accountType)) {
            tableName = "vendor";
        } else {
            return false;
        }

        String sql = "SELECT id FROM " + tableName + " WHERE id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, accountId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void insertReport(Connection connection, String reporterType, int reporterId, String reportedType, int reportedId, String reportOption, String explanation) throws SQLException {
        String sql = "INSERT INTO reports " +
                "(reporter_type, reporter_id, reported_type, reported_id, report_option, explanation, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'Submitted', NOW())";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, reporterType);
            preparedStatement.setInt(2, reporterId);
            preparedStatement.setString(3, reportedType);
            preparedStatement.setInt(4, reportedId);
            preparedStatement.setString(5, reportOption);
            preparedStatement.setString(6, explanation);
            preparedStatement.executeUpdate();
        }
    }

    private Report getReportTargetForUpdate(Connection connection, int reportId) throws SQLException {
        String sql = "SELECT id, reported_type, reported_id FROM reports WHERE id = ? LIMIT 1 FOR UPDATE";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, reportId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    Report report = new Report();
                    report.setId(resultSet.getInt("id"));
                    report.setReportedType(safe(resultSet.getString("reported_type")).toLowerCase());
                    report.setReportedId(resultSet.getInt("reported_id"));
                    return report;
                }
            }
        }

        return null;
    }

    private void suspendCustomer(Connection connection, int customerId, int suspendDays, String reason) throws SQLException {
        String sql = "UPDATE customer " +
                "SET status = 'Suspended', suspendstartdate = NOW(), suspendenddate = DATE_ADD(NOW(), INTERVAL ? DAY), banreason = ? " +
                "WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, suspendDays);
            preparedStatement.setString(2, safe(reason));
            preparedStatement.setInt(3, customerId);
            preparedStatement.executeUpdate();
        }
    }

    private void suspendVendor(Connection connection, int vendorId, int suspendDays, String reason) throws SQLException {
        String sql = "UPDATE vendor " +
                "SET status = 'Suspended', suspendstartdate = NOW(), suspendenddate = DATE_ADD(NOW(), INTERVAL ? DAY), banreason = ? " +
                "WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, suspendDays);
            preparedStatement.setString(2, safe(reason));
            preparedStatement.setInt(3, vendorId);
            preparedStatement.executeUpdate();
        }
    }

    private void banCustomer(Connection connection, int customerId, String reason) throws SQLException {
        String sql = "UPDATE customer " +
                "SET status = 'Banned', suspendstartdate = NULL, suspendenddate = NULL, banreason = ? " +
                "WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, safe(reason));
            preparedStatement.setInt(2, customerId);
            preparedStatement.executeUpdate();
        }
    }

    private void banVendor(Connection connection, int vendorId, String reason) throws SQLException {
        String sql = "UPDATE vendor " +
                "SET status = 'Banned', suspendstartdate = NULL, suspendenddate = NULL, banreason = ? " +
                "WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, safe(reason));
            preparedStatement.setInt(2, vendorId);
            preparedStatement.executeUpdate();
        }
    }

    private void updateReportAfterAction(Connection connection, int reportId, String status, String actionTaken, int adminId, String adminNote) throws SQLException {
        String sql = "UPDATE reports " +
                "SET status = ?, admin_note = ?, action_taken = ?, action_admin_id = ?, action_date = NOW() " +
                "WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, status);
            preparedStatement.setString(2, safe(adminNote));
            preparedStatement.setString(3, actionTaken);
            preparedStatement.setInt(4, adminId);
            preparedStatement.setInt(5, reportId);
            preparedStatement.executeUpdate();
        }
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}