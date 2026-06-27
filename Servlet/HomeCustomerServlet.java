package Servlet;

import DAO.HomeCustomerDAO;
import Model.CustomerHomeData;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import Servlet.SessionUtil;

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

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "index.html")) {
            return;
        }

        try {
            int userId = Integer.parseInt(session.getAttribute("userId").toString());
            String customerName = session.getAttribute("userName") != null ? session.getAttribute("userName").toString() : "User";

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

            out.print(gson.toJson(homeData));

        } catch (Exception e) {
            Map<String, String> errorRes = new HashMap<>();
            errorRes.put("status", "error");
            errorRes.put("message", "Database Error: " + e.getMessage());
            out.print(gson.toJson(errorRes));
        }

        out.flush();
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