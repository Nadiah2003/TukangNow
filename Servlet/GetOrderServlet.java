package Servlet;

import DAO.OrderDAO;
import Model.Order;
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

@WebServlet("/GetOrderServlet")
public class GetOrderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private OrderDAO orderDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        orderDAO = new OrderDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        setJsonResponse(response);

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "index.html")) {
            return;
        }

        try {
            int custId = Integer.parseInt(session.getAttribute("userId").toString());
            String action = safe(request.getParameter("action"));

            if ("status_list".equalsIgnoreCase(action)) {
                List<String> statusList = orderDAO.getBookingStatusList(custId);

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("statuses", statusList);

                sendJson(response, gson.toJson(result));
                return;
            }

            String fStatus = safe(request.getParameter("status"));
            String fService = safe(request.getParameter("service"));
            String fDate = safe(request.getParameter("date"));

            List<Order> orderList = orderDAO.getFilteredOrders(custId, fStatus, fService, fDate);
            sendJson(response, gson.toJson(orderList));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private String errorResponse(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "error");
        result.put("message", message == null ? "Unable to load orders." : message);
        return gson.toJson(result);
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private void sendJson(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
}