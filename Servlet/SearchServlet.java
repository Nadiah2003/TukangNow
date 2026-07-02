package Servlet;

import DAO.SearchDAO;
import Model.VendorSearchResult;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "SearchServlet", urlPatterns = {"/SearchServlet"})
public class SearchServlet extends HttpServlet {

    private final SearchDAO searchDAO = new SearchDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        setJsonResponse(response);

        PrintWriter out = response.getWriter();

        try {
            String serviceName = safe(request.getParameter("serviceName"));
            String dateInput = safe(request.getParameter("date"));
            String radiusParam = safe(request.getParameter("radius"));
            String latParam = safe(request.getParameter("latitude"));
            String lngParam = safe(request.getParameter("longitude"));

            if (serviceName.isEmpty() || dateInput.isEmpty() || latParam.isEmpty() || lngParam.isEmpty()) {
                out.print(gson.toJson(new ArrayList<VendorSearchResult>()));
                return;
            }

            double radiusInput = radiusParam.isEmpty() ? 10.0 : Double.parseDouble(radiusParam);
            double userLat = Double.parseDouble(latParam);
            double userLng = Double.parseDouble(lngParam);

            if (radiusInput <= 0) {
                radiusInput = 10.0;
            }

            List<VendorSearchResult> results = searchDAO.searchNearbyVendors(serviceName, dateInput, radiusInput, userLat, userLng);
            out.print(gson.toJson(results));

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(errorResponse("Invalid latitude, longitude or radius format.")));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(errorResponse(e.getMessage() == null ? "Unable to search vendor." : e.getMessage())));
        } finally {
            out.flush();
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With");
    }

    private Map<String, String> errorResponse(String message) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("status", "error");
        map.put("message", message);
        return map;
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}