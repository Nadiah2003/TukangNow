package Servlet;

import DAO.HomeCustomerDAO;
import Model.CustomerHomeData;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/api/home-cust")
public class HomeCustomerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private HomeCustomerDAO homeCustomerDAO;
    private Gson gson;

    @Override
    public void init() {
        homeCustomerDAO = new HomeCustomerDAO();
        gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        setJsonResponse(response);

        PrintWriter out = response.getWriter();
        int userId = getLoginUserId(request);

        if (userId <= 0) {
            out.print(gson.toJson(errorResponse("Session not found. Customer login session is missing. Please login again.")));
            out.flush();
            return;
        }

        try {
            String action = request.getParameter("action");

            if ("notifications".equalsIgnoreCase(action)) {
                JsonObject notificationResult = new JsonObject();
                notificationResult.addProperty("status", "success");
                notificationResult.add("notifications", gson.toJsonTree(homeCustomerDAO.getCustomerNotifications(userId)));
                notificationResult.addProperty("unreadNotificationsCount", homeCustomerDAO.getCustomerNotificationCount(userId));
                out.print(gson.toJson(notificationResult));
                out.flush();
                return;
            }

            String customerName = getLoginUserName(request);

            if (customerName.trim().isEmpty()) {
                customerName = "User";
            }

            String[] customerMetadata = homeCustomerDAO.getCustomerProfileAndState(userId);
            String customerImg = customerMetadata[0];
            int rewardsPoints = Integer.parseInt(customerMetadata[1]);
            double customerLatitude = parseDouble(customerMetadata[3]);
            double customerLongitude = parseDouble(customerMetadata[4]);

            int electricalRange = sanitizeRange(request.getParameter("electricalRange"));
            int plumberRange = sanitizeRange(request.getParameter("plumberRange"));
            int lawnRange = sanitizeRange(request.getParameter("lawnRange"));

            String electricalSort = sanitizeSort(request.getParameter("electricalSort"));
            String plumberSort = sanitizeSort(request.getParameter("plumberSort"));
            String lawnSort = sanitizeSort(request.getParameter("lawnSort"));

            String walletBalance = homeCustomerDAO.getWalletBalance(userId);

            CustomerHomeData homeData = new CustomerHomeData();
            homeData.setStatus("success");
            homeData.setCustomerName(customerName);
            homeData.setCustomerImg(customerImg);
            homeData.setWalletBalance(walletBalance);
            homeData.setRewardsPoints(rewardsPoints);
            homeData.setEvents(homeCustomerDAO.getActiveEvents(userId));
            homeData.setElectricians(homeCustomerDAO.getVendorsByCategoryAndRange("Electrical", customerLatitude, customerLongitude, electricalRange, electricalSort));
            homeData.setPlumbers(homeCustomerDAO.getVendorsByCategoryAndRange("Plumber", customerLatitude, customerLongitude, plumberRange, plumberSort));
            homeData.setLawns(homeCustomerDAO.getVendorsByCategoryAndRange("Lawn Mower", customerLatitude, customerLongitude, lawnRange, lawnSort));

            JsonObject result = gson.toJsonTree(homeData).getAsJsonObject();
            result.add("notifications", gson.toJsonTree(homeCustomerDAO.getCustomerNotifications(userId)));
            result.addProperty("unreadNotificationsCount", homeCustomerDAO.getCustomerNotificationCount(userId));

            out.print(gson.toJson(result));

        } catch (Exception e) {
            out.print(gson.toJson(errorResponse("Database Error: " + e.getMessage())));
        }

        out.flush();
    }

    private int getLoginUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("userId") != null) {
            try {
                return Integer.parseInt(session.getAttribute("userId").toString());
            } catch (Exception e) {
                return 0;
            }
        }

        String cookieUserId = getCookieValue(request, "tn_userId");

        if (!cookieUserId.trim().isEmpty()) {
            try {
                return Integer.parseInt(cookieUserId.trim());
            } catch (Exception e) {
                return 0;
            }
        }

        String requestUserId = request.getParameter("userId");

        if (requestUserId == null || requestUserId.trim().isEmpty()) {
            requestUserId = request.getParameter("customerId");
        }

        try {
            return Integer.parseInt(requestUserId);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getLoginUserName(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("userName") != null) {
            return session.getAttribute("userName").toString();
        }

        return getCookieValue(request, "tn_userName");
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return "";
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                try {
                    return URLDecoder.decode(cookie.getValue(), "UTF-8");
                } catch (Exception e) {
                    return cookie.getValue();
                }
            }
        }

        return "";
    }

    private Map<String, String> errorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
    }

    private int sanitizeRange(String value) {
        int range = 50;

        try {
            if (value != null && !value.trim().isEmpty()) {
                range = Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException e) {
            range = 50;
        }

        if (range <= 5) {
            return 5;
        }

        if (range <= 10) {
            return 10;
        }

        if (range <= 20) {
            return 20;
        }

        if (range <= 30) {
            return 30;
        }

        return 50;
    }

    private String sanitizeSort(String value) {
        if (value != null && value.trim().equalsIgnoreCase("desc")) {
            return "desc";
        }

        return "asc";
    }

    private double parseDouble(String value) {
        try {
            if (value != null && !value.trim().isEmpty()) {
                return Double.parseDouble(value.trim());
            }
        } catch (NumberFormatException e) {
            return 0.0;
        }

        return 0.0;
    }
}