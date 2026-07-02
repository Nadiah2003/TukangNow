package Servlet;

import DAO.VendorListDAO;
import Model.VendorListItem;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/VendorListServlet")
public class VendorListServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private VendorListDAO vendorListDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        vendorListDAO = new VendorListDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "index.html")) {
            return;
        }

        try {
            int customerId = Integer.parseInt(session.getAttribute("userId").toString());
            String type = safe(request.getParameter("type"));

            if (type.isEmpty()) {
                sendJson(response, "[]");
                return;
            }

            double minRating = parseDouble(request.getParameter("min_rating"), 0.0);
            Double maxPrice = parseNullableDouble(request.getParameter("max_price"));
            String sortBy = safeDefault(request.getParameter("sort_by"), "distance");
            double radius = normalizeRadius(parseDouble(request.getParameter("radius"), 50.0));

            List<VendorListItem> vendors = vendorListDAO.getVendorList(customerId, type, minRating, maxPrice, sortBy, radius);
            sendJson(response, gson.toJson(vendors));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private double normalizeRadius(double radius) {
        if (radius <= 0) {
            return 50.0;
        }

        if (radius > 50.0) {
            return 50.0;
        }

        return radius;
    }

    private double parseDouble(String value, double fallback) {
        try {
            if (value != null && !value.trim().isEmpty()) {
                return Double.parseDouble(value.trim());
            }
        } catch (NumberFormatException e) {
            return fallback;
        }

        return fallback;
    }

    private Double parseNullableDouble(String value) {
        try {
            if (value != null && !value.trim().isEmpty()) {
                return Double.parseDouble(value.trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }

        return null;
    }

    private String errorResponse(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "error");
        map.put("message", message == null ? "Unable to load vendor list." : message);
        return gson.toJson(map);
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private String safeDefault(String value, String fallback) {
        String clean = safe(value);

        if (clean.isEmpty()) {
            return fallback;
        }

        return clean;
    }

    private void sendJson(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
}