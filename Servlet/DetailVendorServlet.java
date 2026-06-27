package Servlet;

import DAO.DetailVendorDAO;
import Model.DetailVendor;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import Servlet.SessionUtil;

@WebServlet("/DetailVendorServlet")
public class DetailVendorServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DetailVendorDAO detailVendorDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() {
        detailVendorDAO = new DetailVendorDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "login.html")) {
            return;
        }

        String action = request.getParameter("action");
        String idParam = request.getParameter("id");

        try {
            if (action == null || !action.equals("get")) {
                sendJson(response, errorResponse("Invalid action."));
                return;
            }

            if (idParam == null || idParam.trim().isEmpty()) {
                sendJson(response, errorResponse("Missing vendor id."));
                return;
            }

            int vendorId = Integer.parseInt(idParam);
            DetailVendor vendor = detailVendorDAO.getVendorApplication(vendorId);

            if (vendor == null) {
                sendJson(response, errorResponse("Vendor not found."));
                return;
            }

            vendor.setStatus("success");
            vendor.setMessage("Vendor loaded successfully.");
            sendJson(response, gson.toJson(vendor));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse("Database Error: " + e.getMessage()));
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

        Integer adminId = getSessionUserId(request);

        if (adminId == null) {
            sendJson(response, errorResponse("Unauthorized access. Please login again."));
            return;
        }

        String action = request.getParameter("action");
        String idParam = request.getParameter("id");

        try {
            if (idParam == null || idParam.trim().isEmpty()) {
                sendJson(response, errorResponse("Missing vendor id."));
                return;
            }

            int vendorId = Integer.parseInt(idParam);

            if ("approve".equals(action)) {
                String expiry = request.getParameter("expiry");
                String servicesParam = request.getParameter("services");

                if (expiry == null || expiry.trim().isEmpty()) {
                    sendJson(response, errorResponse("Missing expiry date."));
                    return;
                }

                if (servicesParam == null || servicesParam.trim().isEmpty()) {
                    sendJson(response, errorResponse("Missing service list."));
                    return;
                }

                List<String> services = Arrays.asList(servicesParam.split(","));
                boolean success = detailVendorDAO.approveVendor(vendorId, expiry, services, adminId);

                if (success) {
                    sendJson(response, successResponse("Vendor approved successfully."));
                } else {
                    sendJson(response, errorResponse("Approval failed."));
                }

                return;
            }

            if ("updateStatus".equals(action)) {
                String status = request.getParameter("status");

                if (status == null || status.trim().isEmpty()) {
                    sendJson(response, errorResponse("Missing status."));
                    return;
                }

                boolean success = detailVendorDAO.updateVendorStatus(vendorId, status, adminId);

                if (success) {
                    sendJson(response, successResponse("Vendor status updated successfully."));
                } else {
                    sendJson(response, errorResponse("Status update failed."));
                }

                return;
            }

            sendJson(response, errorResponse("Invalid action."));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse("Database Error: " + e.getMessage()));
        }
    }

    private Integer getSessionUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            return null;
        }

        try {
            return Integer.parseInt(session.getAttribute("userId").toString());
        } catch (NumberFormatException e) {
            return null;
        }
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
        map.put("message", message);
        return gson.toJson(map);
    }

    private void sendJson(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
}