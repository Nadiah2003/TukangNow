package Servlet;

import DAO.HomeAdminDAO;
import Model.AdminHomeData;
import Model.AdminHomeVendor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import Servlet.SessionUtil;

@WebServlet("/HomeAdminServlet")
public class HomeAdminServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private HomeAdminDAO homeAdminDAO;

    @Override
    public void init() {
        homeAdminDAO = new HomeAdminDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "login.html")) {
            return;
        }

        int adminId;

        try {
            adminId = Integer.parseInt(session.getAttribute("userId").toString());
        } catch (NumberFormatException e) {
            sendJson(response, "{\"status\":\"session_expired\",\"message\":\"Session expired. Please login again.\",\"redirect\":\"login.html\"}");
            return;
        }

        try {
            AdminHomeData data = homeAdminDAO.getHomeAdminData(adminId);
            sendJson(response, buildDashboardJson(data));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, "{\"error\":\"Database Error: " + jsonEscape(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "login.html")) {
            return;
        }

        doGet(request, response);
    }

    private String buildDashboardJson(AdminHomeData data) {
        StringBuilder json = new StringBuilder();

        json.append("{");
        json.append("\"adminProfile\":\"").append(jsonEscape(data.getAdminProfile())).append("\",");
        json.append("\"newApps\":").append(data.getNewApps()).append(",");
        json.append("\"activeVendors\":").append(data.getActiveVendors()).append(",");
        json.append("\"newReports\":").append(data.getNewReports()).append(",");
        json.append("\"vendors\":[");

        List<AdminHomeVendor> vendors = data.getVendors();

        for (int i = 0; i < vendors.size(); i++) {
            AdminHomeVendor vendor = vendors.get(i);

            json.append("{");
            json.append("\"name\":\"").append(jsonEscape(vendor.getName())).append("\",");
            json.append("\"phone\":\"").append(jsonEscape(vendor.getPhone())).append("\",");
            json.append("\"status\":\"").append(jsonEscape(vendor.getStatus())).append("\",");
            json.append("\"profilePath\":\"").append(jsonEscape(vendor.getProfilePath())).append("\",");
            json.append("\"docUrl\":\"").append(jsonEscape(vendor.getDocUrl())).append("\",");
            json.append("\"rating\":\"").append(jsonEscape(vendor.getRating())).append("\",");
            json.append("\"workDone\":\"").append(jsonEscape(vendor.getWorkDone())).append("\"");
            json.append("}");

            if (i < vendors.size() - 1) {
                json.append(",");
            }
        }

        json.append("]");
        json.append("}");

        return json.toString();
    }

    private void sendJson(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}