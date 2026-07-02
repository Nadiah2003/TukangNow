package Servlet;

import DAO.PaymentDAO;
import Model.PaymentInitialData;
import Model.PaymentProcessResult;
import Model.PaymentRequestData;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

@WebServlet("/PaymentServlet")
@MultipartConfig
public class PaymentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private PaymentDAO paymentDAO;
    private final Gson gson = new Gson();

    private static final String TOYYIBPAY_SECRET_KEY = "vcxi94d9-rvzi-u03z-e7hh-bmw9sryvhhbw";
    private static final String TOYYIBPAY_CATEGORY_CODE = "9gjoqgaj";
    private static final String TOYYIBPAY_CREATE_BILL_URL = "https://dev.toyyibpay.com/index.php/api/createBill";
    private static final String TOYYIBPAY_PAYMENT_URL = "https://dev.toyyibpay.com/";
    private static final String EVIDENCE_UPLOAD_PATH = File.separator + "home" + File.separator + "s72009" + File.separator + "evidence";

    @Override
    public void init() {
        paymentDAO = new PaymentDAO();

        File evidenceDir = new File(EVIDENCE_UPLOAD_PATH);

        if (!evidenceDir.exists()) {
            evidenceDir.mkdirs();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("callback".equalsIgnoreCase(action)) {
            handleCallback(request, response);
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if ("fetch_initial".equalsIgnoreCase(action)) {
            handleFetchInitial(request, response);
            return;
        }

        sendJson(response, errorResponse("Invalid action."));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            sendJson(response, sessionExpiredResponse());
            return;
        }

        try {
            PaymentRequestData requestData = buildPaymentRequest(request, session);

            if (requestData.getAmountOriginal() <= 0) {
                sendJson(response, errorResponse("Payment amount is invalid. Booking was not created."));
                return;
            }

            if (requestData.getBookingId() <= 0 && requestData.getEvidencePath().isEmpty()) {
                String evidencePath = savePendingImages(getFormValue(request, "pending_images_json"));
                requestData.setEvidencePath(evidencePath);
            }

            String generatedOrderId = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase() + "-" + System.currentTimeMillis();
            String method = requestData.getPaymentMethod();

            PaymentProcessResult result;

            if ("e-wallet".equalsIgnoreCase(method)) {
                if (requestData.getPassword() == null || requestData.getPassword().isEmpty()) {
                    sendJson(response, errorResponse("Password not received by server. Please enter password again."));
                    return;
                }

                if (!paymentDAO.isCustomerPasswordValid(requestData.getCustomerId(), requestData.getPassword())) {
                    sendJson(response, errorResponse(paymentDAO.getPasswordFailureMessage(requestData.getCustomerId(), requestData.getPassword())));
                    return;
                }

                result = paymentDAO.processWalletPayment(requestData, generatedOrderId);
                sendJson(response, successResult(result));
                return;
            }

            result = paymentDAO.prepareFpxPayment(requestData, generatedOrderId);
            String billCode = createToyyibPayBill(request, requestData, result);
            paymentDAO.updatePaymentReference(result.getBookingId(), billCode, requestData.getPaymentType());
            result.setPaymentUrl(TOYYIBPAY_PAYMENT_URL + billCode);
            sendJson(response, successResult(result));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void handleFetchInitial(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            sendJson(response, sessionExpiredResponse());
            return;
        }

        try {
            int customerId = Integer.parseInt(session.getAttribute("userId").toString());
            int bookingId = parseInt(request.getParameter("booking_id"));
            PaymentInitialData data = paymentDAO.getInitialData(customerId, bookingId);
            sendJson(response, gson.toJson(data));
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void handleCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean returnPage = "1".equals(request.getParameter("return_page"));

        try {
            int customerId = parseInt(request.getParameter("customer_id"));
            int bookingId = parseInt(request.getParameter("booking_id"));
            double amount = parseDouble(request.getParameter("amount"));
            String paymentType = normalizePaymentType(request.getParameter("payment_type"));

            if (customerId <= 0 || bookingId <= 0) {
                if (returnPage) {
                    response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/myorder.html"));
                    return;
                }

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                sendJson(response, errorResponse("Invalid callback data."));
                return;
            }

            if (isFailedPayment(request)) {
                paymentDAO.markBookingPaymentFailed(bookingId, customerId, paymentType);
            } else {
                paymentDAO.markFpxPaymentSuccess(bookingId, customerId, amount, paymentType);
            }

            if (returnPage) {
                if ("second_payment".equalsIgnoreCase(paymentType)) {
                    String nextUrl = "trackingvendor.html?id=" + bookingId + "&view=customer";
                    String receiptUrl = request.getContextPath() + "/resit.html?bookingId=" + bookingId + "&payment_type=second_payment&next=" + URLEncoder.encode(nextUrl, "UTF-8");
                    response.sendRedirect(response.encodeRedirectURL(receiptUrl));
                    return;
                }

                response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/resit.html?bookingId=" + bookingId + "&payment_type=first_payment"));
                return;
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "success");
            result.put("message", "Callback processed.");
            sendJson(response, gson.toJson(result));

        } catch (Exception e) {
            if (returnPage) {
                response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/myorder.html"));
                return;
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private PaymentRequestData buildPaymentRequest(HttpServletRequest request, HttpSession session) throws IOException, ServletException {
        PaymentRequestData data = new PaymentRequestData();

        String paymentType = safe(getFormValue(request, "payment_type"));

        if (paymentType.isEmpty()) {
            paymentType = safe(getFormValue(request, "type"));
        }

        String vendorIdValue = safe(getFormValue(request, "vendorId"));

        if (vendorIdValue.isEmpty()) {
            vendorIdValue = safe(getFormValue(request, "vendor_id"));
        }

        if (vendorIdValue.isEmpty()) {
            vendorIdValue = "SOS_BROADCAST";
        }

        String serviceIdValue = safe(getFormValue(request, "service_id"));

        if (serviceIdValue.isEmpty()) {
            serviceIdValue = safe(getFormValue(request, "serviceId"));
        }

        data.setCustomerId(Integer.parseInt(session.getAttribute("userId").toString()));
        data.setBookingId(parseInt(getFormValue(request, "booking_id")));
        data.setPaymentMethod(normalizePaymentMethod(getFormValue(request, "payment_method")));
        data.setPaymentType(normalizePaymentType(paymentType));
        data.setAmountOriginal(parseDouble(getFormValue(request, "amount")));
        data.setFinalAmount(parseDouble(getFormValue(request, "final_amount")));
        data.setUsePoints(parseInt(getFormValue(request, "use_points")));
        data.setCustomerVoucherId(parseInt(getFormValue(request, "customer_voucher_id")));
        data.setService(safeDefault(getFormValue(request, "service"), "Service"));
        data.setCategory(safe(getFormValue(request, "category")));
        data.setVendorId(vendorIdValue);
        data.setServiceId(parseInt(serviceIdValue));
        data.setEmergencyServiceIds(safe(getFormValue(request, "emergency_service_ids")));
        data.setProblem(safe(getFormValue(request, "problem")));
        data.setSubservice(safe(getFormValue(request, "subservicebooked")));
        data.setUserName(safeDefault(getFormValue(request, "userName"), "Customer"));
        data.setUserEmail(safeDefault(getFormValue(request, "userEmail"), "customer@email.com"));
        data.setUserPhone(safeDefault(getFormValue(request, "userPhone"), "0123456789"));
        data.setPassword(raw(getFormValue(request, "password")));
        data.setDate(safe(getFormValue(request, "date")));
        data.setTime(safe(getFormValue(request, "time")));
        data.setTravelFee(parseDouble(getFormValue(request, "travelFee")));
        data.setDistanceKm(parseDouble(getFormValue(request, "distanceKm")));
        data.setEvidencePath(safe(getFormValue(request, "evidencePath")));

        if (data.getFinalAmount() < 0) {
            data.setFinalAmount(0);
        }

        if (data.getSubservice().isEmpty()) {
            data.setSubservice(data.getService());
        }

        return data;
    }

    private String getFormValue(HttpServletRequest request, String name) throws IOException, ServletException {
        String value = request.getParameter(name);

        if (value != null) {
            return value;
        }

        String contentType = request.getContentType();

        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            return "";
        }

        Part part;

        try {
            part = request.getPart(name);
        } catch (Exception e) {
            return "";
        }

        if (part == null) {
            return "";
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (java.io.InputStream inputStream = part.getInputStream()) {
            byte[] data = new byte[1024];
            int read;

            while ((read = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, read);
            }
        }

        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private String savePendingImages(String pendingImagesJson) throws Exception {
        if (pendingImagesJson == null || pendingImagesJson.trim().isEmpty() || "[]".equals(pendingImagesJson.trim())) {
            return "";
        }

        File evidenceDir = new File(EVIDENCE_UPLOAD_PATH);

        if (!evidenceDir.exists()) {
            evidenceDir.mkdirs();
        }

        String runtimeEvidencePath = getServletContext().getRealPath("/evidence");
        File runtimeUploadDir = null;

        if (runtimeEvidencePath != null && !runtimeEvidencePath.trim().isEmpty()) {
            runtimeUploadDir = new File(runtimeEvidencePath);

            if (!runtimeUploadDir.exists()) {
                runtimeUploadDir.mkdirs();
            }
        }

        JsonArray images = gson.fromJson(pendingImagesJson, JsonArray.class);
        StringBuilder savedFiles = new StringBuilder();

        for (int i = 0; i < images.size(); i++) {
            JsonObject image = images.get(i).getAsJsonObject();
            String name = image.has("name") && !image.get("name").isJsonNull() ? image.get("name").getAsString() : "";
            String type = image.has("type") && !image.get("type").isJsonNull() ? image.get("type").getAsString() : "";
            String dataUrl = image.has("dataUrl") && !image.get("dataUrl").isJsonNull() ? image.get("dataUrl").getAsString() : "";

            if (dataUrl.isEmpty() || !dataUrl.contains(",")) {
                continue;
            }

            String ext = getSafeExtension(name, type);

            if (!isAllowedImageExtension(ext)) {
                continue;
            }

            String base64Data = dataUrl.substring(dataUrl.indexOf(",") + 1);
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + "_" + i + ext;
            File targetFile = new File(evidenceDir, fileName);

            Files.write(targetFile.toPath(), imageBytes);

            if (runtimeUploadDir != null) {
                File runtimeTargetFile = new File(runtimeUploadDir, fileName);

                if (!runtimeTargetFile.getCanonicalPath().equals(targetFile.getCanonicalPath())) {
                    Files.copy(targetFile.toPath(), runtimeTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            if (savedFiles.length() > 0) {
                savedFiles.append(",");
            }

            savedFiles.append(fileName);
        }

        return savedFiles.toString();
    }

    private String createToyyibPayBill(HttpServletRequest request, PaymentRequestData requestData, PaymentProcessResult result) throws Exception {
        int amountInSen = (int) Math.round(requestData.getFinalAmount() * 100.0);
        int pointsDeducted = 0;

        if ("first_payment".equalsIgnoreCase(requestData.getPaymentType())) {
            try (java.sql.Connection connection = paymentDAO.getConnection()) {
                pointsDeducted = paymentDAO.calculatePointsDeducted(connection, requestData.getCustomerId(), requestData.getUsePoints(), requestData.getAmountOriginal());
            }
        }

        String appBase = request.getScheme() + "://" + request.getServerName() + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort()) + request.getContextPath();
        String rawBillName = "second_payment".equalsIgnoreCase(requestData.getPaymentType()) ? "Balance Payment" : isEmergency(requestData) ? "Emergency " + requestData.getCategory() : "Booking: " + requestData.getService();
        String finalBillName = rawBillName.length() > 30 ? rawBillName.substring(0, 30) : rawBillName;
        String billDescription = "second_payment".equalsIgnoreCase(requestData.getPaymentType()) ? "Second Payment Balance" : "Booking Payment";

        String callbackUrl = appBase + "/PaymentServlet?action=callback"
                + "&customer_id=" + requestData.getCustomerId()
                + "&booking_id=" + result.getBookingId()
                + "&points_deduct=" + pointsDeducted
                + "&cust_voucher_id=" + requestData.getCustomerVoucherId()
                + "&payment_type=" + requestData.getPaymentType()
                + "&amount=" + requestData.getFinalAmount();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("userSecretKey", TOYYIBPAY_SECRET_KEY);
        params.put("categoryCode", TOYYIBPAY_CATEGORY_CODE);
        params.put("billName", finalBillName);
        params.put("billDescription", billDescription);
        params.put("billPriceSetting", "1");
        params.put("billPayorInfo", "1");
        params.put("billAmount", String.valueOf(amountInSen));
        params.put("billReturnUrl", callbackUrl + "&return_page=1");
        params.put("billCallbackUrl", callbackUrl);
        params.put("billExternalReferenceNo", result.getOrderId());
        params.put("billTo", cut(requestData.getUserName(), 30));
        params.put("billEmail", requestData.getUserEmail());
        params.put("billPhone", requestData.getUserPhone());

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

        throw new Exception("ToyyibPay API Error: " + responseBody.toString());
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

    private String successResult(PaymentProcessResult paymentResult) {
        Map<String, Object> map = new LinkedHashMap<>();
        String paymentUrl = paymentResult.getPaymentUrl();

        if ("second_payment".equalsIgnoreCase(paymentResult.getPaymentType()) && (paymentUrl == null || paymentUrl.trim().isEmpty()) && paymentResult.getBookingId() > 0) {
            paymentUrl = buildSecondPaymentReceiptUrl(paymentResult.getBookingId());
        }

        map.put("status", "success");
        map.put("message", paymentResult.getMessage());
        map.put("payment_type", paymentResult.getPaymentType());
        map.put("order_id", paymentResult.getOrderId());
        map.put("transaction_id", paymentResult.getTransactionId());
        map.put("wallet_transaction_id", paymentResult.getWalletTransactionId());
        map.put("paymentUrl", paymentUrl);
        map.put("payment_url", paymentUrl);
        map.put("redirect", paymentUrl);
        map.put("redirect_url", paymentUrl);
        map.put("method", paymentResult.getMethod());
        map.put("booking_id", paymentResult.getBookingId());
        return gson.toJson(map);
    }

    private String buildSecondPaymentReceiptUrl(int bookingId) {
        String nextUrl = "trackingvendor.html?id=" + bookingId + "&view=customer";
        return "resit.html?bookingId=" + bookingId + "&payment_type=second_payment&next=" + encodeUrlParam(nextUrl);
    }

    private String encodeUrlParam(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private String errorResponse(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "error");
        map.put("message", message == null ? "Unknown error." : message);
        return gson.toJson(map);
    }

    private String sessionExpiredResponse() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "session_expired");
        map.put("message", "Session expired. Please login again.");
        map.put("redirect", "index.html");
        return gson.toJson(map);
    }

    private boolean isEmergency(PaymentRequestData requestData) {
        return "Emergency".equalsIgnoreCase(safe(requestData.getService()))
                || "Emergency".equalsIgnoreCase(safe(requestData.getCategory()))
                || !safe(requestData.getEmergencyServiceIds()).isEmpty();
    }

    private String getSafeExtension(String fileName, String type) {
        String cleanName = safe(fileName).toLowerCase();

        if (cleanName.endsWith(".jpg")) {
            return ".jpg";
        }

        if (cleanName.endsWith(".jpeg")) {
            return ".jpeg";
        }

        if (cleanName.endsWith(".png")) {
            return ".png";
        }

        if (cleanName.endsWith(".webp")) {
            return ".webp";
        }

        String cleanType = safe(type).toLowerCase();

        if (cleanType.contains("jpeg")) {
            return ".jpg";
        }

        if (cleanType.contains("png")) {
            return ".png";
        }

        if (cleanType.contains("webp")) {
            return ".webp";
        }

        return ".jpg";
    }

    private boolean isAllowedImageExtension(String ext) {
        return ".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext) || ".webp".equals(ext);
    }

    private String normalizePaymentMethod(String method) {
        String clean = safe(method).toLowerCase();

        if ("ewallet".equals(clean) || "e-wallet".equals(clean) || "wallet".equals(clean)) {
            return "e-wallet";
        }

        return "fpx";
    }

    private String normalizePaymentType(String type) {
        String clean = safe(type).toLowerCase().replace("-", "_").replace(" ", "_");

        if ("second_payment".equals(clean) || "balance".equals(clean)) {
            return "second_payment";
        }

        return "first_payment";
    }

    private String cut(String value, int maxLength) {
        String clean = safe(value);

        if (clean.length() <= maxLength) {
            return clean;
        }

        return clean.substring(0, maxLength);
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

    private String raw(String value) {
        if (value == null) {
            return "";
        }

        return value;
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