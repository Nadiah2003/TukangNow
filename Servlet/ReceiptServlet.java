package Servlet;

import DAO.ReceiptDAO;
import Model.ReceiptData;
import com.google.gson.Gson;
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

@WebServlet("/ReceiptServlet")
public class ReceiptServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private ReceiptDAO receiptDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() {
        receiptDAO = new ReceiptDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "index.html")) {
            return;
        }

        String idOrRef = request.getParameter("idOrRef");

        if (idOrRef == null || idOrRef.trim().isEmpty()) {
            idOrRef = request.getParameter("bookingId");
        }

        if (idOrRef == null || idOrRef.trim().isEmpty()) {
            sendJson(response, errorResponse("ID tempahan atau rujukan tidak dibekalkan."));
            return;
        }

        String paymentType = request.getParameter("payment_type");

        if (paymentType == null || paymentType.trim().isEmpty()) {
            paymentType = request.getParameter("type");
        }

        if (paymentType == null || paymentType.trim().isEmpty()) {
            paymentType = "first_payment";
        }

        String nextUrl = request.getParameter("next");

        try {
            int customerId = Integer.parseInt(session.getAttribute("userId").toString());
            ReceiptData receiptData = receiptDAO.getReceiptData(customerId, idOrRef, paymentType);

            if (receiptData == null) {
                sendJson(response, errorResponse("Rekod tidak dijumpai dalam sistem pangkalan data booking."));
                return;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "success");
            result.put("payment_type", normalizePaymentType(paymentType));
            result.put("next_url", nextUrl == null ? "" : nextUrl.trim());
            result.put("data", receiptData);

            sendJson(response, gson.toJson(result));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private String normalizePaymentType(String type) {
        String clean = type == null ? "" : type.trim().toLowerCase().replace("-", "_").replace(" ", "_");

        if ("second_payment".equals(clean) || "balance".equals(clean)) {
            return "second_payment";
        }

        return "first_payment";
    }

    private String errorResponse(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "error");
        map.put("message", message == null ? "Unknown error." : message);
        return gson.toJson(map);
    }

    private void sendJson(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
}