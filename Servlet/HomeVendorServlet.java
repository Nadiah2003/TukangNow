package Servlet;

import DAO.HomeVendorDAO;
import Model.HomeVendorData;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import Servlet.SessionUtil;

@WebServlet("/HomeVendorServlet")
public class HomeVendorServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private HomeVendorDAO homeVendorDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() {
        homeVendorDAO = new HomeVendorDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "login.html")) {
            return;
        }

        Integer vendorId = getSessionUserId(request);

        if (vendorId == null) {
            sendJson(response, errorResponse("Session expired."));
            return;
        }

        try {
            HomeVendorData data = homeVendorDAO.getDashboardData(vendorId);
            sendJson(response, gson.toJson(data));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "login.html")) {
            return;
        }

        Integer vendorId = getSessionUserId(request);

        if (vendorId == null) {
            sendJson(response, errorResponse("Session expired."));
            return;
        }

        try {
            JsonObject jsonObject = gson.fromJson(request.getReader(), JsonObject.class);

            if (jsonObject == null || !jsonObject.has("action")) {
                sendJson(response, errorResponse("Invalid request."));
                return;
            }

            String action = jsonObject.get("action").getAsString();

            if ("respondNow".equals(action)) {
                if (!jsonObject.has("bookingId")) {
                    sendJson(response, errorResponse("Invalid Booking ID."));
                    return;
                }

                int bookingId = jsonObject.get("bookingId").getAsInt();

                if (bookingId <= 0) {
                    sendJson(response, errorResponse("Invalid Booking ID."));
                    return;
                }

                String resultMessage = homeVendorDAO.respondNow(vendorId, bookingId);

                if ("success".equals(resultMessage)) {
                    sendJson(response, successResponse("Booking successfully accepted!"));
                } else {
                    sendJson(response, errorResponse(resultMessage));
                }

                return;
            }

            if ("rejectBooking".equals(action)) {
                if (!jsonObject.has("bookingId")) {
                    sendJson(response, errorResponse("Invalid Booking ID."));
                    return;
                }

                int bookingId = jsonObject.get("bookingId").getAsInt();

                if (bookingId <= 0) {
                    sendJson(response, errorResponse("Invalid Booking ID."));
                    return;
                }

                String resultMessage = homeVendorDAO.rejectBooking(vendorId, bookingId);

                if ("success".equals(resultMessage)) {
                    sendJson(response, successResponse("Booking successfully rejected."));
                } else {
                    sendJson(response, errorResponse(resultMessage));
                }

                return;
            }

            sendJson(response, errorResponse("Invalid action."));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse(e.getMessage()));
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

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
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

    private void sendJson(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
}