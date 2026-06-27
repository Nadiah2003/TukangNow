package Servlet;

import DAO.BookingDAO;
import Model.BookingPageData;
import Model.EstimateItem;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
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

@WebServlet("/BookingServlet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 40
)
public class BookingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private BookingDAO bookingDAO;
    private final Gson gson = new Gson();

    private static final String EVIDENCE_UPLOAD_PATH = "C:" + File.separator + "xampp" + File.separator + "htdocs" + File.separator + "TukangNow" + File.separator + "src" + File.separator + "main" + File.separator + "webapp" + File.separator + "evidence";

    @Override
    public void init() {
        bookingDAO = new BookingDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            sendJson(response, errorResponse("Invalid session. Please login again."));
            return;
        }

        try {
            int customerId = Integer.parseInt(session.getAttribute("userId").toString());
            String vendorIdParam = request.getParameter("vendorId");
            String selectedDate = request.getParameter("date");

            if (vendorIdParam == null || vendorIdParam.trim().isEmpty()) {
                sendJson(response, errorResponse("Missing vendor ID."));
                return;
            }

            int vendorId = Integer.parseInt(vendorIdParam.trim());
            BookingPageData data = bookingDAO.getBookingPageData(customerId, vendorId, selectedDate);

            if (data == null) {
                sendJson(response, errorResponse("Vendor not found."));
                return;
            }

            data.setStatus("success");
            sendJson(response, gson.toJson(data));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            sendJson(response, errorResponse("Invalid session. Please login again."));
            return;
        }

        String action = request.getParameter("action");

        if ("estimate".equalsIgnoreCase(action)) {
            handleEstimate(request, response);
            return;
        }

        try {
            int customerId = Integer.parseInt(session.getAttribute("userId").toString());

            String vendorIdParam = request.getParameter("vendorId");
            String subserviceBooked = request.getParameter("service");
            String date = request.getParameter("date");
            String time = request.getParameter("time");
            String depositParam = request.getParameter("amount");
            String problem = request.getParameter("details");
            String travelFeeParam = request.getParameter("travelFee");
            String distanceKmParam = request.getParameter("distanceKm");
            String paymentStatus = request.getParameter("paymentStatus");
            String paymentReference = request.getParameter("paymentReference");

            if (vendorIdParam == null || vendorIdParam.trim().isEmpty()
                    || subserviceBooked == null || subserviceBooked.trim().isEmpty()
                    || date == null || date.trim().isEmpty()
                    || time == null || time.trim().isEmpty()) {
                sendJson(response, errorResponse("Missing booking details."));
                return;
            }

            int vendorId = Integer.parseInt(vendorIdParam.trim());
            double deposit = parseDouble(depositParam);
            double travelFee = parseDouble(travelFeeParam);
            double distanceKm = parseDouble(distanceKmParam);

            if (paymentStatus == null || paymentStatus.trim().isEmpty()) {
                paymentStatus = "Pending";
            }

            if (paymentReference == null || paymentReference.trim().isEmpty()) {
                paymentReference = "N/A";
            }

            File evidenceDir = new File(EVIDENCE_UPLOAD_PATH);

            if (!evidenceDir.exists()) {
                evidenceDir.mkdirs();
            }

            List<String> uploadedFiles = saveEvidenceFiles(request.getParts(), evidenceDir);
            String evidencePath = String.join(",", uploadedFiles);

            int bookingId = bookingDAO.createBooking(
                    customerId,
                    vendorId,
                    subserviceBooked,
                    date,
                    time,
                    deposit,
                    problem,
                    travelFee,
                    distanceKm,
                    paymentStatus,
                    paymentReference,
                    evidencePath
            );

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "success");
            result.put("bookingId", bookingId);
            result.put("amount", deposit);

            sendJson(response, gson.toJson(result));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void handleEstimate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String serviceName = request.getParameter("service");
        String vendorIdParam = request.getParameter("vendorId");

        if (serviceName == null || serviceName.trim().isEmpty()) {
            sendJson(response, errorResponse("Missing service name."));
            return;
        }

        if (vendorIdParam == null || vendorIdParam.trim().isEmpty()) {
            sendJson(response, errorResponse("Missing vendor ID."));
            return;
        }

        try {
            int vendorId = Integer.parseInt(vendorIdParam.trim());
            List<EstimateItem> items = bookingDAO.getEstimateItems(serviceName.trim(), vendorId);
            double itemSubtotal = 0.00;

            for (EstimateItem item : items) {
                itemSubtotal += item.getItemPrice();
            }

            itemSubtotal = Math.round(itemSubtotal * 100.0) / 100.0;

            Map<String, Object> estimate = new LinkedHashMap<>();
            estimate.put("service", serviceName.trim());
            estimate.put("items", items);
            estimate.put("itemSubtotal", itemSubtotal);
            estimate.put("note", "This is an estimated possible item breakdown based on stored price catalogue. Actual item used and final price may vary after vendor inspection.");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "success");
            result.put("estimate", estimate);

            sendJson(response, gson.toJson(result));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private List<String> saveEvidenceFiles(Collection<Part> parts, File evidenceDir) throws IOException {
        List<String> uploadedFiles = new ArrayList<>();
        int index = 0;

        for (Part part : parts) {
            if ("images".equals(part.getName())
                    && part.getSize() > 0
                    && part.getSubmittedFileName() != null
                    && !part.getSubmittedFileName().trim().isEmpty()) {

                String originalName = sanitizeFileName(part.getSubmittedFileName());
                String ext = getFileExtension(originalName).toLowerCase();

                if (isAllowedImageExtension(ext)) {
                    String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + "_" + index + ext;
                    File targetFile = new File(evidenceDir, fileName);
                    part.write(targetFile.getAbsolutePath());
                    uploadedFiles.add(fileName);
                    index++;
                }
            }
        }

        return uploadedFiles;
    }

    private boolean isAllowedImageExtension(String ext) {
        return ".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext) || ".webp".equals(ext);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "";
        }

        return new File(fileName).getName().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf("."));
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

    private String errorResponse(String message) {
        Map<String, String> map = new LinkedHashMap<>();
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