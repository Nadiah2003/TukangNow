package DAO;

import Config.ConnectionManager;
import Model.Report;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ReportDAO {

    private Connection getConnection() throws SQLException {
        Connection connection = ConnectionManager.getConnection();

        if (connection == null) {
            throw new SQLException("Database connection is null.");
        }

        return connection;
    }

    public String submitReportFromBooking(int sessionUserId, String sessionRole, int bookingId, String reportType, String explanation) throws SQLException {
        String cleanSessionRole = safe(sessionRole).toLowerCase();
        String cleanReportType = safe(reportType);
        String cleanExplanation = safe(explanation);

        if (sessionUserId <= 0) {
            return "Session expired.";
        }

        if (bookingId <= 0) {
            return "Booking ID not found. Please open report from tracking page.";
        }

        if (cleanReportType.isEmpty()) {
            return "Please choose a report type.";
        }

        if (cleanExplanation.isEmpty()) {
            return "Please write your explanation.";
        }

        if (cleanExplanation.length() < 10) {
            return "Explanation must be at least 10 characters.";
        }

        if (cleanExplanation.length() > 1000) {
            return "Explanation cannot exceed 1000 characters.";
        }

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                ExistingReport existingReport = getExistingReportForBooking(connection, bookingId);
                BookingReportInfo bookingInfo = getBookingReportInfoForUpdate(connection, bookingId);

                if (bookingInfo == null) {
                    connection.rollback();
                    return "Booking ID not found.";
                }

                String reporterType;
                int reporterId;
                String reportedType;
                int reportedId;

                if ("customer".equals(cleanSessionRole)) {
                    if (sessionUserId != bookingInfo.customerId) {
                        connection.rollback();
                        return "You do not have access to this booking.";
                    }

                    reporterType = "customer";
                    reporterId = bookingInfo.customerId;
                    reportedType = "vendor";
                    reportedId = bookingInfo.vendorId;

                } else if ("vendor".equals(cleanSessionRole)) {
                    if (sessionUserId != bookingInfo.vendorId) {
                        connection.rollback();
                        return "You do not have access to this booking.";
                    }

                    reporterType = "vendor";
                    reporterId = bookingInfo.vendorId;
                    reportedType = "customer";
                    reportedId = bookingInfo.customerId;

                } else {
                    if (sessionUserId == bookingInfo.customerId) {
                        reporterType = "customer";
                        reporterId = bookingInfo.customerId;
                        reportedType = "vendor";
                        reportedId = bookingInfo.vendorId;
                    } else if (sessionUserId == bookingInfo.vendorId) {
                        reporterType = "vendor";
                        reporterId = bookingInfo.vendorId;
                        reportedType = "customer";
                        reportedId = bookingInfo.customerId;
                    } else {
                        connection.rollback();
                        return "You do not have access to this booking.";
                    }
                }

                if (existingReport != null) {
                    if (existingReport.reporterId == reporterId && existingReport.reporterType.equalsIgnoreCase(reporterType)) {
                        connection.rollback();
                        return "You already submitted a report for this booking.";
                    }

                    connection.rollback();
                    return "This booking has already been reported and is under admin review.";
                }

                if (!isValidAccount(connection, reporterType, reporterId)) {
                    connection.rollback();
                    return "Invalid reporter account.";
                }

                if (!isValidAccount(connection, reportedType, reportedId)) {
                    connection.rollback();
                    return "Reported account not found.";
                }

                insertReport(connection, reporterType, reporterId, reportedType, reportedId, bookingId, cleanReportType, cleanExplanation);
                updateBookingStatusToReport(connection, bookingId);

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

    public Report getBookingReportForUser(int sessionUserId, String sessionRole, int bookingId) throws SQLException {
        if (sessionUserId <= 0 || bookingId <= 0) {
            return null;
        }

        String cleanRole = safe(sessionRole).toLowerCase();

        String sql = reportSelectSql() +
                "WHERE r.booking_id = ? " +
                "AND (" +
                "? = 'admin' " +
                "OR (r.reporter_type = ? AND r.reporter_id = ?) " +
                ") " +
                "ORDER BY r.id DESC LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, bookingId);
            preparedStatement.setString(2, cleanRole);
            preparedStatement.setString(3, cleanRole);
            preparedStatement.setInt(4, sessionUserId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapReport(resultSet);
                }
            }
        }

        return null;
    }

    public boolean isReportedAccountForBooking(int sessionUserId, String sessionRole, int bookingId) throws SQLException {
        if (sessionUserId <= 0 || bookingId <= 0) {
            return false;
        }

        String cleanRole = safe(sessionRole).toLowerCase();

        String sql = "SELECT id FROM reports " +
                "WHERE booking_id = ? " +
                "AND reported_type = ? " +
                "AND reported_id = ? " +
                "LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, bookingId);
            preparedStatement.setString(2, cleanRole);
            preparedStatement.setInt(3, sessionUserId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
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

            if (report.getBookingId() > 0 && !isValidBooking(connection, report.getBookingId())) {
                return "Booking ID not found.";
            }

            insertReport(connection, reporterType, report.getReporterId(), reportedType, report.getReportedId(), report.getBookingId(), reportOption, explanation);

            if (report.getBookingId() > 0) {
                updateBookingStatusToReport(connection, report.getBookingId());
            }
        }

        return "success";
    }

    public ArrayList<Report> getAllReports() throws SQLException {
        ArrayList<Report> reports = new ArrayList<>();

        String sql = reportSelectSql() + " ORDER BY r.created_at DESC, r.id DESC";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                reports.add(mapReport(resultSet));
            }
        }

        return reports;
    }

    public Report getReportDetails(int reportId) throws SQLException {
        if (reportId <= 0) {
            return null;
        }

        String sql = reportSelectSql() + " WHERE r.id = ? LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, reportId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapReport(resultSet);
                }
            }
        }

        return null;
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
                "SET status = 'Resolved', admin_note = ?, action_taken = 'Case Closed', action_admin_id = ?, action_date = NOW() " +
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

    public String closeCaseNoSuspend(int reportId, int adminId, String adminNote) throws SQLException {
        if (reportId <= 0) {
            return "Invalid report ID.";
        }

        if (safe(adminNote).isEmpty()) {
            return "Close case reason cannot be empty.";
        }

        String sql = "UPDATE reports " +
                "SET status = 'Resolved', admin_note = ?, action_taken = 'No Suspension / Case Closed', action_admin_id = ?, action_date = NOW() " +
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

        if (safe(adminNote).isEmpty()) {
            return "Suspend reason cannot be empty.";
        }

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                Report target = getReportTargetForUpdate(connection, reportId);

                if (target == null) {
                    connection.rollback();
                    return "Report not found.";
                }

                int currentSuspendCount = getSuspendCount(connection, target.getReportedType(), target.getReportedId());

                if (currentSuspendCount >= 3) {
                    connection.rollback();
                    return "This account has reached 3 suspensions. Permanent ban only.";
                }

                if ("customer".equals(target.getReportedType())) {
                    suspendCustomer(connection, target.getReportedId(), suspendDays, adminNote);
                } else if ("vendor".equals(target.getReportedType())) {
                    suspendVendor(connection, target.getReportedId(), suspendDays, adminNote);
                } else {
                    connection.rollback();
                    return "Invalid reported account type.";
                }

                updateReportAfterAction(connection, reportId, "Action Taken", "Suspended " + suspendDays + " day(s)", adminId, adminNote);

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

                updateReportAfterAction(connection, reportId, "Action Taken", "Permanent Ban", adminId, adminNote);

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

    private BookingReportInfo getBookingReportInfoForUpdate(Connection connection, int bookingId) throws SQLException {
        String sql = "SELECT b.id, b.customer_id, s.vendor_id " +
                "FROM booking b " +
                "JOIN service s ON b.service_id = s.id " +
                "WHERE b.id = ? " +
                "LIMIT 1 FOR UPDATE";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    BookingReportInfo info = new BookingReportInfo();
                    info.bookingId = resultSet.getInt("id");
                    info.customerId = resultSet.getInt("customer_id");
                    info.vendorId = resultSet.getInt("vendor_id");
                    return info;
                }
            }
        }

        return null;
    }

    private ExistingReport getExistingReportForBooking(Connection connection, int bookingId) throws SQLException {
        String sql = "SELECT reporter_type, reporter_id, reported_type, reported_id " +
                "FROM reports " +
                "WHERE booking_id = ? " +
                "ORDER BY id DESC LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    ExistingReport report = new ExistingReport();
                    report.reporterType = safe(resultSet.getString("reporter_type"));
                    report.reporterId = resultSet.getInt("reporter_id");
                    report.reportedType = safe(resultSet.getString("reported_type"));
                    report.reportedId = resultSet.getInt("reported_id");
                    return report;
                }
            }
        }

        return null;
    }

    private void updateBookingStatusToReport(Connection connection, int bookingId) throws SQLException {
        String sql = "UPDATE booking SET status = 'Report' WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);
            preparedStatement.executeUpdate();
        }
    }

    private String reportSelectSql() {
        return "SELECT " +
                "r.id, IFNULL(r.booking_id, 0) AS booking_id, r.reporter_type, r.reporter_id, r.reported_type, r.reported_id, " +
                "r.report_option, r.explanation, r.status, IFNULL(r.admin_note, '') AS admin_note, IFNULL(r.action_taken, '') AS action_taken, " +
                "IFNULL(r.action_admin_id, 0) AS action_admin_id, " +
                "IFNULL(DATE_FORMAT(r.action_date, '%Y-%m-%d %H:%i'), '') AS action_date, " +
                "IFNULL(DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i'), '') AS created_at, " +

                "CASE WHEN r.reporter_type = 'customer' THEN IFNULL(rc.name, 'Unknown Customer') ELSE IFNULL(rv.name, 'Unknown Vendor') END AS reporter_name, " +
                "CASE WHEN r.reporter_type = 'customer' THEN IFNULL(rc.email, '') ELSE IFNULL(rv.email, '') END AS reporter_email, " +
                "CASE WHEN r.reporter_type = 'customer' THEN IFNULL(rc.nophone, '') ELSE IFNULL(rv.nophone, '') END AS reporter_phone, " +
                "CASE WHEN r.reporter_type = 'customer' THEN CONCAT_WS(', ', NULLIF(rc.address, ''), NULLIF(rc.postcode, ''), NULLIF(rc.city, ''), NULLIF(rc.state, ''), NULLIF(rc.country, '')) ELSE CONCAT_WS(', ', NULLIF(rv.address, ''), NULLIF(rv.postcode, ''), NULLIF(rv.city, ''), NULLIF(rv.state, ''), NULLIF(rv.country, '')) END AS reporter_address, " +
                "CASE WHEN r.reporter_type = 'customer' THEN IFNULL(rc.status, 'Active') ELSE IFNULL(rv.status, '-') END AS reporter_account_status, " +
                "CASE WHEN r.reporter_type = 'customer' THEN IFNULL(rc.suspendcount, 0) ELSE IFNULL(rv.suspendcount, 0) END AS reporter_suspend_count, " +

                "CASE WHEN r.reported_type = 'customer' THEN IFNULL(tc.name, 'Unknown Customer') ELSE IFNULL(tv.name, 'Unknown Vendor') END AS reported_name, " +
                "CASE WHEN r.reported_type = 'customer' THEN IFNULL(tc.email, '') ELSE IFNULL(tv.email, '') END AS reported_email, " +
                "CASE WHEN r.reported_type = 'customer' THEN IFNULL(tc.nophone, '') ELSE IFNULL(tv.nophone, '') END AS reported_phone, " +
                "CASE WHEN r.reported_type = 'customer' THEN CONCAT_WS(', ', NULLIF(tc.address, ''), NULLIF(tc.postcode, ''), NULLIF(tc.city, ''), NULLIF(tc.state, ''), NULLIF(tc.country, '')) ELSE CONCAT_WS(', ', NULLIF(tv.address, ''), NULLIF(tv.postcode, ''), NULLIF(tv.city, ''), NULLIF(tv.state, ''), NULLIF(tv.country, '')) END AS reported_address, " +
                "CASE WHEN r.reported_type = 'customer' THEN IFNULL(tc.status, 'Active') ELSE IFNULL(tv.status, '-') END AS reported_account_status, " +
                "CASE WHEN r.reported_type = 'customer' THEN IFNULL(tc.suspendcount, 0) ELSE IFNULL(tv.suspendcount, 0) END AS reported_suspend_count, " +
                "CASE WHEN r.reported_type = 'customer' THEN IFNULL(DATE_FORMAT(tc.suspendstartdate, '%Y-%m-%d %H:%i'), '') ELSE IFNULL(DATE_FORMAT(tv.suspendstartdate, '%Y-%m-%d %H:%i'), '') END AS reported_suspend_start_date, " +
                "CASE WHEN r.reported_type = 'customer' THEN IFNULL(DATE_FORMAT(tc.suspendenddate, '%Y-%m-%d %H:%i'), '') ELSE IFNULL(DATE_FORMAT(tv.suspendenddate, '%Y-%m-%d %H:%i'), '') END AS reported_suspend_end_date, " +
                "CASE WHEN r.reported_type = 'customer' THEN IFNULL(tc.banreason, '') ELSE IFNULL(tv.banreason, '') END AS reported_ban_reason, " +

                "IFNULL(DATE_FORMAT(b.bookingdate, '%Y-%m-%d %H:%i'), '') AS booking_date, " +
                "IFNULL(b.subservicebooked, '') AS subservicebooked, " +
                "IFNULL(b.problem, '') AS problem, " +
                "IFNULL(b.status, '') AS booking_status, " +
                "IFNULL(b.deposit, 0) AS deposit, " +
                "IFNULL(b.totalamount, 0) AS totalamount, " +
                "IFNULL(b.travelfee, 0) AS travelfee, " +
                "IFNULL(b.materialcost, 0) AS materialcost, " +
                "IFNULL(b.totalbalance, 0) AS totalbalance, " +
                "IFNULL(b.distancekm, 0) AS distancekm, " +
                "IFNULL(b.evidencepath, '') AS evidencepath " +

                "FROM reports r " +
                "LEFT JOIN customer rc ON r.reporter_type = 'customer' AND r.reporter_id = rc.id " +
                "LEFT JOIN vendor rv ON r.reporter_type = 'vendor' AND r.reporter_id = rv.id " +
                "LEFT JOIN customer tc ON r.reported_type = 'customer' AND r.reported_id = tc.id " +
                "LEFT JOIN vendor tv ON r.reported_type = 'vendor' AND r.reported_id = tv.id " +
                "LEFT JOIN booking b ON r.booking_id = b.id ";
    }

    private Report mapReport(ResultSet resultSet) throws SQLException {
        Report report = new Report();

        report.setId(resultSet.getInt("id"));
        report.setBookingId(resultSet.getInt("booking_id"));

        report.setReporterType(safe(resultSet.getString("reporter_type")));
        report.setReporterId(resultSet.getInt("reporter_id"));
        report.setReporterName(safe(resultSet.getString("reporter_name")));
        report.setReporterEmail(safe(resultSet.getString("reporter_email")));
        report.setReporterPhone(safe(resultSet.getString("reporter_phone")));
        report.setReporterAddress(safe(resultSet.getString("reporter_address")));
        report.setReporterAccountStatus(safe(resultSet.getString("reporter_account_status")));
        report.setReporterSuspendCount(resultSet.getInt("reporter_suspend_count"));

        report.setReportedType(safe(resultSet.getString("reported_type")));
        report.setReportedId(resultSet.getInt("reported_id"));
        report.setReportedName(safe(resultSet.getString("reported_name")));
        report.setReportedEmail(safe(resultSet.getString("reported_email")));
        report.setReportedPhone(safe(resultSet.getString("reported_phone")));
        report.setReportedAddress(safe(resultSet.getString("reported_address")));
        report.setReportedAccountStatus(safe(resultSet.getString("reported_account_status")));
        report.setReportedSuspendCount(resultSet.getInt("reported_suspend_count"));
        report.setReportedSuspendStartDate(safe(resultSet.getString("reported_suspend_start_date")));
        report.setReportedSuspendEndDate(safe(resultSet.getString("reported_suspend_end_date")));
        report.setReportedBanReason(safe(resultSet.getString("reported_ban_reason")));

        report.setReportOption(safe(resultSet.getString("report_option")));
        report.setExplanation(safe(resultSet.getString("explanation")));
        report.setStatus(safe(resultSet.getString("status")));
        report.setAdminNote(safe(resultSet.getString("admin_note")));
        report.setActionTaken(safe(resultSet.getString("action_taken")));
        report.setActionAdminId(resultSet.getInt("action_admin_id"));
        report.setActionDate(safe(resultSet.getString("action_date")));
        report.setCreatedAt(safe(resultSet.getString("created_at")));

        report.setBookingDate(safe(resultSet.getString("booking_date")));
        report.setSubserviceBooked(safe(resultSet.getString("subservicebooked")));
        report.setProblem(safe(resultSet.getString("problem")));
        report.setBookingStatus(safe(resultSet.getString("booking_status")));
        report.setDeposit(resultSet.getDouble("deposit"));
        report.setTotalAmount(resultSet.getDouble("totalamount"));
        report.setTravelFee(resultSet.getDouble("travelfee"));
        report.setMaterialCost(resultSet.getDouble("materialcost"));
        report.setTotalBalance(resultSet.getDouble("totalbalance"));
        report.setDistanceKm(resultSet.getDouble("distancekm"));
        report.setEvidencePath(safe(resultSet.getString("evidencepath")));

        return report;
    }

    private boolean isValidAccount(Connection connection, String accountType, int accountId) throws SQLException {
        if ("customer".equals(accountType)) {
            String sql = "SELECT id FROM customer WHERE id = ? LIMIT 1";

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, accountId);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    return resultSet.next();
                }
            }
        }

        if ("vendor".equals(accountType)) {
            String sql = "SELECT id FROM vendor WHERE id = ? LIMIT 1";

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, accountId);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    return resultSet.next();
                }
            }
        }

        return false;
    }

    private boolean isValidBooking(Connection connection, int bookingId) throws SQLException {
        String sql = "SELECT id FROM booking WHERE id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, bookingId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void insertReport(Connection connection, String reporterType, int reporterId, String reportedType, int reportedId, int bookingId, String reportOption, String explanation) throws SQLException {
        String sql = "INSERT INTO reports " +
                "(reporter_type, reporter_id, reported_type, reported_id, booking_id, report_option, explanation, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'Submitted', NOW())";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, reporterType);
            preparedStatement.setInt(2, reporterId);
            preparedStatement.setString(3, reportedType);
            preparedStatement.setInt(4, reportedId);

            if (bookingId > 0) {
                preparedStatement.setInt(5, bookingId);
            } else {
                preparedStatement.setNull(5, java.sql.Types.INTEGER);
            }

            preparedStatement.setString(6, reportOption);
            preparedStatement.setString(7, explanation);
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

    private int getSuspendCount(Connection connection, String accountType, int accountId) throws SQLException {
        String sql;

        if ("customer".equals(accountType)) {
            sql = "SELECT IFNULL(suspendcount, 0) AS suspendcount FROM customer WHERE id = ? LIMIT 1 FOR UPDATE";
        } else if ("vendor".equals(accountType)) {
            sql = "SELECT IFNULL(suspendcount, 0) AS suspendcount FROM vendor WHERE id = ? LIMIT 1 FOR UPDATE";
        } else {
            return 0;
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, accountId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("suspendcount");
                }
            }
        }

        return 0;
    }

    private void suspendCustomer(Connection connection, int customerId, int suspendDays, String reason) throws SQLException {
        String sql = "UPDATE customer " +
                "SET status = 'Suspended', suspendstartdate = NOW(), suspendenddate = DATE_ADD(NOW(), INTERVAL ? DAY), banreason = ?, suspendcount = IFNULL(suspendcount, 0) + 1 " +
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
                "SET status = 'Suspended', suspendstartdate = NOW(), suspendenddate = DATE_ADD(NOW(), INTERVAL ? DAY), banreason = ?, suspendcount = IFNULL(suspendcount, 0) + 1 " +
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

    private static class BookingReportInfo {
        private int bookingId;
        private int customerId;
        private int vendorId;
    }

    private static class ExistingReport {
        private String reporterType;
        private int reporterId;
        private String reportedType;
        private int reportedId;
    }
}