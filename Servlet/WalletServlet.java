package Servlet;

import DAO.WalletDAO;
import Model.WalletData;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import Servlet.SessionUtil;

@WebServlet("/WalletServlet")
public class WalletServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private WalletDAO walletDAO;
    private final Gson gson = new Gson();

    private static final String TOYYIBPAY_SECRET_KEY = "vcxi94d9-rvzi-u03z-e7hh-bmw9sryvhhbw";
    private static final String TOYYIBPAY_CATEGORY_CODE = "9gjoqgaj";
    private static final String TOYYIBPAY_CREATE_BILL_URL = "https://dev.toyyibpay.com/index.php/api/createBill";
    private static final String TOYYIBPAY_PAYMENT_URL = "https://dev.toyyibpay.com/";

    @Override
    public void init() {
        walletDAO = new WalletDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");

        if ("getWallet".equalsIgnoreCase(action)) {
            HttpSession session = request.getSession(false);

            if (SessionUtil.isSessionExpired(session, response, "userId", "index.html")) {
                return;
            }

            handleGetWallet(request, response, session);
            return;
        }

        if ("topup_callback".equalsIgnoreCase(action)) {
            handleTopupCallback(request, response);
            return;
        }

        if ("topup_return".equalsIgnoreCase(action)) {
            handleTopupReturn(request, response);
            return;
        }

        sendJson(response, errorResponse("Invalid action."));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "index.html")) {
            return;
        }

        String action = request.getParameter("action");

        if ("topup".equalsIgnoreCase(action)) {
            handleTopup(request, response, session);
            return;
        }

        if ("transfer_to_bank".equalsIgnoreCase(action)) {
            handleTransferToBank(request, response, session);
            return;
        }

        sendJson(response, errorResponse("Invalid action."));
    }

    private void handleGetWallet(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws IOException {
        try {
            int customerId = Integer.parseInt(session.getAttribute("userId").toString());
            WalletData data = walletDAO.getWalletData(customerId);
            sendJson(response, gson.toJson(data));
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void handleTopup(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws IOException {
        try {
            int customerId = Integer.parseInt(session.getAttribute("userId").toString());
            double amount = parseDouble(request.getParameter("amount"));

            if (amount < 10) {
                sendJson(response, errorResponse("Minimum top up value is RM 10.00"));
                return;
            }

            String referenceId = "WL-" + System.currentTimeMillis();
            String billCode = createToyyibPayWalletBill(request, customerId, amount, referenceId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "success");
            result.put("redirect", true);
            result.put("url", TOYYIBPAY_PAYMENT_URL + billCode);

            sendJson(response, gson.toJson(result));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void handleTransferToBank(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws IOException {
        try {
            int customerId = Integer.parseInt(session.getAttribute("userId").toString());
            double amount = parseDouble(request.getParameter("amount"));
            String bankName = safe(request.getParameter("bank_name"));
            String accountNo = safe(request.getParameter("account_no"));

            walletDAO.transferToBank(customerId, amount, bankName, accountNo);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "success");
            result.put("message", "RM " + String.format("%.2f", amount) + " successfully transferred to your " + bankName + " account.");

            sendJson(response, gson.toJson(result));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void handleTopupCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            int customerId = parseInt(request.getParameter("customer_id"));
            double amount = parseDouble(request.getParameter("amount"));
            String referenceId = safe(request.getParameter("reference_id"));

            if (customerId <= 0 || amount <= 0 || referenceId.isEmpty()) {
                sendJson(response, errorResponse("Invalid top up callback data."));
                return;
            }

            if (isFailedPayment(request)) {
                sendJson(response, successResponse("Top up callback failed status received."));
                return;
            }

            walletDAO.processTopupSuccess(customerId, amount, referenceId);
            sendJson(response, successResponse("Top up callback processed."));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void handleTopupReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            int customerId = parseInt(request.getParameter("customer_id"));
            double amount = parseDouble(request.getParameter("amount"));
            String referenceId = safe(request.getParameter("reference_id"));

            if (customerId > 0 && amount > 0 && !referenceId.isEmpty() && !isFailedPayment(request)) {
                walletDAO.processTopupSuccess(customerId, amount, referenceId);
                response.sendRedirect("wallet.html?topup=success");
                return;
            }

            response.sendRedirect("wallet.html?topup=failed");

        } catch (Exception e) {
            response.sendRedirect("wallet.html?topup=failed");
        }
    }

    private String createToyyibPayWalletBill(HttpServletRequest request, int customerId, double amount, String referenceId) throws Exception {
        int amountInSen = (int) Math.round(amount * 100.0);
        String appBase = request.getScheme() + "://" + request.getServerName() + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort()) + request.getContextPath();

        String returnUrl = appBase + "/WalletServlet?action=topup_return"
                + "&customer_id=" + customerId
                + "&amount=" + amount
                + "&reference_id=" + encode(referenceId);

        String callbackUrl = appBase + "/WalletServlet?action=topup_callback"
                + "&customer_id=" + customerId
                + "&amount=" + amount
                + "&reference_id=" + encode(referenceId);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("userSecretKey", TOYYIBPAY_SECRET_KEY);
        params.put("categoryCode", TOYYIBPAY_CATEGORY_CODE);
        params.put("billName", "TukangNow Wallet Top Up");
        params.put("billDescription", "Top Up for Customer ID: " + customerId);
        params.put("billPriceSetting", "1");
        params.put("billPayorInfo", "1");
        params.put("billAmount", String.valueOf(amountInSen));
        params.put("billReturnUrl", returnUrl);
        params.put("billCallbackUrl", callbackUrl);
        params.put("billExternalReferenceNo", referenceId);
        params.put("billTo", "Customer ID " + customerId);
        params.put("billEmail", "customer@tukangnow.com");
        params.put("billPhone", "0123456789");

        String postData = buildFormBody(params);

        URL url = new URL(TOYYIBPAY_CREATE_BILL_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);

        try (OutputStream outputStream = connection.getOutputStream()) {
            byte[] input = postData.getBytes(StandardCharsets.UTF_8);
            outputStream.write(input, 0, input.length);
        }

        int statusCode = connection.getResponseCode();
        BufferedReader reader;

        if (statusCode >= 200 && statusCode < 300) {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
        }

        StringBuilder responseBody = new StringBuilder();

        try (BufferedReader bufferedReader = reader) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                responseBody.append(line);
            }
        }

        JsonElement root = gson.fromJson(responseBody.toString(), JsonElement.class);

        if (root != null && root.isJsonArray()) {
            JsonArray array = root.getAsJsonArray();

            if (array.size() > 0 && array.get(0).isJsonObject()) {
                JsonObject first = array.get(0).getAsJsonObject();

                if (first.has("BillCode") && !first.get("BillCode").isJsonNull()) {
                    return first.get("BillCode").getAsString();
                }

                if (first.has("msg") && !first.get("msg").isJsonNull()) {
                    throw new Exception(first.get("msg").getAsString());
                }
            }
        }

        if (root != null && root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();

            if (object.has("msg") && !object.get("msg").isJsonNull()) {
                throw new Exception(object.get("msg").getAsString());
            }
        }

        throw new Exception("ToyyibPay API Error");
    }

    private boolean isFailedPayment(HttpServletRequest request) {
        String statusId = safe(request.getParameter("status_id"));
        String billPaymentStatus = safe(request.getParameter("billpaymentStatus"));
        String transactionStatus = safe(request.getParameter("transaction_status"));
        String paymentStatus = safe(request.getParameter("payment_status"));

        return "3".equals(statusId)
                || "3".equals(billPaymentStatus)
                || transactionStatus.toLowerCase().contains("fail")
                || transactionStatus.toLowerCase().contains("cancel")
                || paymentStatus.toLowerCase().contains("fail")
                || paymentStatus.toLowerCase().contains("cancel");
    }

    private String buildFormBody(Map<String, String> params) throws Exception {
        StringBuilder body = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (body.length() > 0) {
                body.append("&");
            }

            body.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            body.append("=");
            body.append(URLEncoder.encode(entry.getValue() == null ? "" : entry.getValue(), "UTF-8"));
        }

        return body.toString();
    }

    private String successResponse(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "success");
        map.put("message", message);
        return gson.toJson(map);
    }

    private String errorResponse(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "error");
        map.put("message", message == null ? "Unknown error." : message);
        return gson.toJson(map);
    }

    private String encode(String value) throws Exception {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    private int parseInt(String value) {
        try {
            if (value != null && !value.trim().isEmpty()) {
                return Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException e) {
            return 0;
        }

        return 0;
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