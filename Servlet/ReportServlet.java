package Servlet;

import DAO.ReportDAO;
import Model.Report;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "ReportServlet", urlPatterns = {"/ReportServlet"})
public class ReportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private ReportDAO reportDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        reportDAO = new ReportDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        HttpSession session = request.getSession(false);

        if (session == null || getSessionUserId(session) == null) {
            sendJson(response, sessionExpiredResponse(request));
            return;
        }

        String action = request.getParameter("action");

        try {
            if ("session".equals(action)) {
                sendJson(response, sessionResponse(session));
                return;
            }

            if ("bookingReport".equals(action)) {
                int bookingId = getRequestInt(request, "bookingId");
                int userId = getSessionUserId(session);
                String role = getSessionRole(session);

                Report report = reportDAO.getBookingReportForUser(userId, role, bookingId);

                if (report != null || isAdminSession(session)) {
                    sendJson(response, bookingReportResponse(report));
                    return;
                }

                if (reportDAO.isReportedAccountForBooking(userId, role, bookingId)) {
                    sendJson(response, reportedAccountResponse("This booking has been reported and is under admin review. You cannot take action on this booking."));
                    return;
                }

                sendJson(response, errorResponse("Report details are only available to the account that submitted the report."));
                return;
            }

            if ("list".equals(action)) {
                if (!isAdminSession(session)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    sendJson(response, errorResponse("Access denied. Admin only."));
                    return;
                }

                ArrayList<Report> reports = reportDAO.getAllReports();
                sendJson(response, reportsResponse(reports));
                return;
            }

            if ("detail".equals(action)) {
                if (!isAdminSession(session)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    sendJson(response, errorResponse("Access denied. Admin only."));
                    return;
                }

                int reportId = getRequestInt(request, "reportId");
                Report report = reportDAO.getReportDetails(reportId);

                if (report == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    sendJson(response, errorResponse("Report not found."));
                    return;
                }

                sendJson(response, reportResponse(report));
                return;
            }

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, errorResponse("Invalid action."));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse(e.getMessage() == null ? "Unable to load reports." : e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null || getSessionUserId(session) == null) {
            sendJson(response, sessionExpiredResponse(request));
            return;
        }

        Integer userId = getSessionUserId(session);

        if (userId == null || userId <= 0) {
            sendJson(response, sessionExpiredResponse(request));
            return;
        }

        try {
            JsonObject jsonObject = gson.fromJson(request.getReader(), JsonObject.class);

            if (jsonObject == null || !jsonObject.has("action")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendJson(response, errorResponse("Invalid request."));
                return;
            }

            String action = getString(jsonObject, "action");

            if ("submitReport".equals(action)) {
                submitReport(jsonObject, userId, session, response);
                return;
            }

            if (!isAdminSession(session)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                sendJson(response, errorResponse("Access denied. Admin only."));
                return;
            }

            if ("investigate".equals(action)) {
                int reportId = getInt(jsonObject, "reportId");
                String adminNote = getString(jsonObject, "adminNote");
                String result = reportDAO.markInvestigating(reportId, userId, adminNote);
                sendActionResult(response, result, "Report marked as investigating.");
                return;
            }

            if ("resolve".equals(action)) {
                int reportId = getInt(jsonObject, "reportId");
                String adminNote = getString(jsonObject, "adminNote");
                String result = reportDAO.resolveReport(reportId, userId, adminNote);
                sendActionResult(response, result, "Case closed successfully.");
                return;
            }

            if ("noSuspendClose".equals(action)) {
                int reportId = getInt(jsonObject, "reportId");
                String adminNote = getString(jsonObject, "adminNote");
                String result = reportDAO.closeCaseNoSuspend(reportId, userId, adminNote);
                sendActionResult(response, result, "Case closed with no suspension.");
                return;
            }

            if ("suspend".equals(action)) {
                int reportId = getInt(jsonObject, "reportId");
                int suspendDays = getInt(jsonObject, "suspendDays");
                String adminNote = getString(jsonObject, "adminNote");
                String result = reportDAO.suspendReportedAccount(reportId, userId, suspendDays, adminNote);
                sendActionResult(response, result, "Reported account suspended successfully.");
                return;
            }

            if ("ban".equals(action)) {
                int reportId = getInt(jsonObject, "reportId");
                String adminNote = getString(jsonObject, "adminNote");
                String result = reportDAO.banReportedAccount(reportId, userId, adminNote);
                sendActionResult(response, result, "Reported account permanently banned successfully.");
                return;
            }

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, errorResponse("Invalid action."));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse(e.getMessage() == null ? "Unable to process report." : e.getMessage()));
        }
    }

    private void submitReport(JsonObject jsonObject, int userId, HttpSession session, HttpServletResponse response) throws IOException {
        int bookingId = getInt(jsonObject, "bookingId");
        String reportType = getString(jsonObject, "reportType");

        if (reportType.isEmpty()) {
            reportType = getString(jsonObject, "reportOption");
        }

        String explanation = getString(jsonObject, "explanation");
        String sessionRole = getSessionRole(session);

        try {
            String result = reportDAO.submitReportFromBooking(userId, sessionRole, bookingId, reportType, explanation);

            if ("success".equals(result)) {
                sendJson(response, successResponse("Your report has been submitted successfully. This booking status has been changed to Report."));
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendJson(response, errorResponse(result));
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse(e.getMessage() == null ? "Unable to submit report." : e.getMessage()));
        }
    }

    private void sendActionResult(HttpServletResponse response, String result, String successMessage) throws IOException {
        if ("success".equals(result)) {
            sendJson(response, successResponse(successMessage));
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, errorResponse(result));
        }
    }

    private boolean isAdminSession(HttpSession session) {
        if (session == null) {
            return false;
        }

        String[] adminIdKeys = {"adminId", "admin_id", "adminID"};

        for (String key : adminIdKeys) {
            Object value = session.getAttribute(key);

            if (value != null) {
                try {
                    int parsed = Integer.parseInt(value.toString());

                    if (parsed > 0) {
                        return true;
                    }
                } catch (Exception e) {
                }
            }
        }

        String[] roleKeys = {"role", "userRole", "accountType", "userType", "loginType"};

        for (String key : roleKeys) {
            Object value = session.getAttribute(key);

            if (value != null && "admin".equalsIgnoreCase(value.toString().trim())) {
                return true;
            }
        }

        return false;
    }

    private String getSessionRole(HttpSession session) {
        if (session == null) {
            return "";
        }

        String[] roleKeys = {"role", "userRole", "accountType", "userType", "loginType"};

        for (String key : roleKeys) {
            Object value = session.getAttribute(key);

            if (value != null) {
                String role = value.toString().trim().toLowerCase();

                if ("customer".equals(role) || "vendor".equals(role) || "admin".equals(role)) {
                    return role;
                }
            }
        }

        if (session.getAttribute("customerId") != null) {
            return "customer";
        }

        if (session.getAttribute("vendorId") != null) {
            return "vendor";
        }

        return "";
    }

    private Integer getSessionUserId(HttpSession session) {
        String[] keys = {"userId", "adminId", "admin_id", "adminID", "customerId", "vendorId", "id"};

        for (String key : keys) {
            Object value = session.getAttribute(key);

            if (value != null) {
                try {
                    int parsed = Integer.parseInt(value.toString());

                    if (parsed > 0) {
                        return parsed;
                    }
                } catch (Exception e) {
                }
            }
        }

        return null;
    }

    private String sessionResponse(HttpSession session) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "success");
        map.put("isAdmin", isAdminSession(session));
        map.put("userId", getSessionUserId(session));
        map.put("role", getSessionRole(session));
        return gson.toJson(map);
    }

    private String getString(JsonObject jsonObject, String key) {
        if (!jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return "";
        }

        return jsonObject.get(key).getAsString().trim();
    }

    private int getInt(JsonObject jsonObject, String key) {
        try {
            if (!jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
                return 0;
            }

            return jsonObject.get(key).getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }

    private int getRequestInt(HttpServletRequest request, String key) {
        try {
            return Integer.parseInt(request.getParameter(key));
        } catch (Exception e) {
            return 0;
        }
    }

    private String reportsResponse(ArrayList<Report> reports) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "success");
        map.put("reports", reports);
        return gson.toJson(map);
    }

    private String reportResponse(Report report) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "success");
        map.put("report", report);
        return gson.toJson(map);
    }

    private String bookingReportResponse(Report report) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (report == null) {
            map.put("status", "error");
            map.put("message", "Report not found.");
        } else {
            map.put("status", "success");
            map.put("report", report);
        }

        return gson.toJson(map);
    }

    private String reportedAccountResponse(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "reported_account");
        map.put("message", message);
        return gson.toJson(map);
    }

    private String successResponse(String message) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("status", "success");
        map.put("message", message);
        return gson.toJson(map);
    }

    private String errorResponse(String message) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("status", "error");
        map.put("message", message == null ? "Something went wrong." : message);
        return gson.toJson(map);
    }

    private String sessionExpiredResponse(HttpServletRequest request) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("status", "session_expired");
        map.put("redirect", request.getContextPath() + "/login.html");
        return gson.toJson(map);
    }

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private void sendJson(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
}