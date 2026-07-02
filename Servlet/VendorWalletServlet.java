package Servlet;

import DAO.VendorWalletDAO;
import Model.VendorWallet;
import Model.VendorWalletTransaction;
import com.google.gson.Gson;
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

@WebServlet(name = "VendorWalletServlet", urlPatterns = {"/VendorWalletServlet"})
public class VendorWalletServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final double MIN_WITHDRAW_AMOUNT = 10.00;

    private VendorWalletDAO vendorWalletDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        vendorWalletDAO = new VendorWalletDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            sendJson(response, sessionExpiredResponse(request));
            return;
        }

        try {
            int vendorId = Integer.parseInt(session.getAttribute("userId").toString());
            sendJson(response, successResponse(vendorId, null));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse(e.getMessage() == null ? "Unable to load wallet." : e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            sendJson(response, sessionExpiredResponse(request));
            return;
        }

        String action = request.getParameter("action");

        if (action == null || !action.equals("withdraw")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, errorResponse("Invalid action."));
            return;
        }

        try {
            int vendorId = Integer.parseInt(session.getAttribute("userId").toString());
            String amountText = request.getParameter("amount");

            if (amountText == null || amountText.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendJson(response, errorResponse("Invalid withdraw amount."));
                return;
            }

            double amount = Double.parseDouble(amountText);

            if (amount < MIN_WITHDRAW_AMOUNT) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendJson(response, errorResponse("Minimum withdraw amount is RM 10.00."));
                return;
            }

            vendorWalletDAO.withdraw(vendorId, amount);

            sendJson(response, successResponse(vendorId, "Withdraw successful."));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, errorResponse("Invalid withdraw amount."));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, errorResponse(e.getMessage() == null ? "Withdraw failed." : e.getMessage()));
        }
    }

    private String successResponse(int vendorId, String message) throws Exception {
        VendorWallet wallet = vendorWalletDAO.getWalletSummary(vendorId);
        ArrayList<VendorWalletTransaction> transactions = vendorWalletDAO.getWalletTransactions(vendorId);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "success");

        if (message != null) {
            map.put("message", message);
        }

        map.put("wallet", wallet);
        map.put("transactions", transactions);

        return gson.toJson(map);
    }

    private String sessionExpiredResponse(HttpServletRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "session_expired");
        map.put("redirect", request.getContextPath() + "/login.html");
        return gson.toJson(map);
    }

    private String errorResponse(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "error");
        map.put("message", message);
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