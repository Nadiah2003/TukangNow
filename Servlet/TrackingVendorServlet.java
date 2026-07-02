package Servlet;

import DAO.TrackingVendorDAO;
import Model.TrackingMaterialItem;
import Model.TrackingVendorData;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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

@WebServlet("/TrackingVendorServlet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 80
)
public class TrackingVendorServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private TrackingVendorDAO trackingVendorDAO;
    private final Gson gson = new Gson();

    private static final String RESIT_UPLOAD_PATH = File.separator + "home" + File.separator + "s72009" + File.separator + "resit";

    @Override
    public void init() throws ServletException {
        trackingVendorDAO = new TrackingVendorDAO();

        File resitDir = new File(RESIT_UPLOAD_PATH);

        if (!resitDir.exists()) {
            resitDir.mkdirs();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        String view = safe(request.getParameter("view"));
        String redirectPage = "customer".equalsIgnoreCase(view) ? "index.html" : "login.html";

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", redirectPage)) {
            return;
        }

        try {
            int sessionUserId = Integer.parseInt(session.getAttribute("userId").toString());
            int bookingId = parseInt(request.getParameter("id"));

            if (bookingId <= 0) {
                sendJson(response, errorResponse("Booking ID diperlukan."));
                return;
            }

            if (view.isEmpty()) {
                view = getSessionRole(session);
            }

            TrackingVendorData data = trackingVendorDAO.getTrackingData(sessionUserId, view, bookingId);

            if (data == null) {
                sendJson(response, errorResponse("Data tidak dijumpai atau anda tiada akses untuk booking ini."));
                return;
            }

            sendJson(response, gson.toJson(data));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        setJsonResponse(response);

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "login.html")) {
            return;
        }

        try {
            int sessionUserId = Integer.parseInt(session.getAttribute("userId").toString());
            String action = safe(request.getParameter("action"));
            int bookingId = parseInt(request.getParameter("booking_id"));

            if (bookingId <= 0) {
                sendJson(response, errorResponse("Invalid booking ID."));
                return;
            }

            if ("update_location".equalsIgnoreCase(action)) {
                handleUpdateLocation(request, response, sessionUserId, bookingId);
                return;
            }

            if ("on_the_way".equalsIgnoreCase(action)) {
                handleOnTheWay(request, response, sessionUserId, bookingId);
                return;
            }

            if ("arrived".equalsIgnoreCase(action)) {
                handleArrived(response, sessionUserId, bookingId);
                return;
            }

            if ("started".equalsIgnoreCase(action)) {
                handleStarted(request, response, sessionUserId, bookingId);
                return;
            }

            if ("completed".equalsIgnoreCase(action)) {
                handleCompleted(request, response, sessionUserId, bookingId);
                return;
            }

            if ("submit_rating".equalsIgnoreCase(action)) {
                handleSubmitRating(request, response, sessionUserId, bookingId);
                return;
            }

            sendJson(response, errorResponse("Invalid action."));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void handleUpdateLocation(HttpServletRequest request, HttpServletResponse response, int vendorId, int bookingId) throws IOException {
        double latitude = parseDouble(request.getParameter("latitude"));
        double longitude = parseDouble(request.getParameter("longitude"));

        if (!isValidMalaysiaCoordinate(latitude, longitude)) {
            sendJson(response, errorResponse("Invalid Malaysia coordinate."));
            return;
        }

        try {
            boolean updated = trackingVendorDAO.updateVendorTrackingLocation(vendorId, bookingId, latitude, longitude);

            if (updated) {
                sendJson(response, successResponse("Location updated."));
            } else {
                sendJson(response, errorResponse("Unable to update location."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void handleOnTheWay(HttpServletRequest request, HttpServletResponse response, int vendorId, int bookingId) throws IOException {
        String vehicleModel = safe(request.getParameter("vehicle_model"));
        String plateNumber = safe(request.getParameter("plate_number"));

        if (vehicleModel.isEmpty() || plateNumber.isEmpty()) {
            sendJson(response, errorResponse("Vehicle type and plate number are required."));
            return;
        }

        try {
            boolean updated = trackingVendorDAO.updateOnTheWay(vendorId, bookingId, vehicleModel, plateNumber);

            if (updated) {
                sendJson(response, successResponse("Status updated to On The Way."));
            } else {
                sendJson(response, errorResponse("Unable to update status. Please check booking status."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void handleArrived(HttpServletResponse response, int vendorId, int bookingId) throws IOException {
        try {
            boolean updated = trackingVendorDAO.updateArrived(vendorId, bookingId);

            if (updated) {
                sendJson(response, successResponse("Status updated to Arrived."));
            } else {
                sendJson(response, errorResponse("Unable to update status to Arrived."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void handleStarted(HttpServletRequest request, HttpServletResponse response, int vendorId, int bookingId) throws IOException, ServletException {
        Part arrivalEvidence = request.getPart("arrival_evidence");

        if (!isValidFilePart(arrivalEvidence)) {
            sendJson(response, errorResponse("Arrival evidence photo is required."));
            return;
        }

        String savedPath = saveUploadFile(arrivalEvidence, "arrival");

        try {
            boolean updated = trackingVendorDAO.updateStarted(vendorId, bookingId, savedPath);

            if (updated) {
                sendJson(response, successResponse("Status updated to Started."));
            } else {
                sendJson(response, errorResponse("Unable to update status to Started."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void handleCompleted(HttpServletRequest request, HttpServletResponse response, int vendorId, int bookingId) throws IOException, ServletException {
        Part completionEvidence = request.getPart("completion_evidence");

        if (!isValidFilePart(completionEvidence)) {
            sendJson(response, errorResponse("Completed work photo is required."));
            return;
        }

        double laborCharge = parseDouble(request.getParameter("labor_charge"));

        if (laborCharge < 0) {
            sendJson(response, errorResponse("Work labour charge is invalid."));
            return;
        }

        int receiptCount = parseInt(request.getParameter("receipt_count"));

        if (receiptCount <= 0) {
            sendJson(response, errorResponse("At least one receipt or shop group is required."));
            return;
        }

        List<TrackingMaterialItem> items = new ArrayList<>();

        for (int r = 0; r < receiptCount; r++) {
            String receiptLabel = safeDefault(request.getParameter("receipt_label_" + r), "Receipt " + (r + 1));
            Part receiptPart = request.getPart("receipt_file_" + r);
            int itemCount = parseInt(request.getParameter("item_count_" + r));

            if (!isValidFilePart(receiptPart)) {
                sendJson(response, errorResponse("Receipt photo is required for " + receiptLabel + "."));
                return;
            }

            if (itemCount <= 0) {
                sendJson(response, errorResponse("Please add at least one item for " + receiptLabel + "."));
                return;
            }

            String receiptPath = saveUploadFile(receiptPart, "receipt");

            for (int i = 0; i < itemCount; i++) {
                String itemName = safe(request.getParameter("item_name_" + r + "_" + i));
                int quantity = parseInt(request.getParameter("item_qty_" + r + "_" + i));
                double price = parseDouble(request.getParameter("item_price_" + r + "_" + i));

                if (itemName.isEmpty()) {
                    sendJson(response, errorResponse("Item name cannot be empty in " + receiptLabel + "."));
                    return;
                }

                if (quantity <= 0) {
                    sendJson(response, errorResponse("Item quantity must be at least 1 in " + receiptLabel + "."));
                    return;
                }

                if (price < 0) {
                    sendJson(response, errorResponse("Item price is invalid in " + receiptLabel + "."));
                    return;
                }

                TrackingMaterialItem item = new TrackingMaterialItem();
                item.setBooking_id(bookingId);
                item.setReceipt_label(receiptLabel);
                item.setItem_name(itemName);
                item.setQuantity(quantity);
                item.setPrice(price);
                item.setReceipt_path(receiptPath);

                items.add(item);
            }
        }

        String completionPath = saveUploadFile(completionEvidence, "completed");

        try {
            boolean updated = trackingVendorDAO.completeWork(vendorId, bookingId, completionPath, laborCharge, items);

            if (updated) {
                sendJson(response, successResponse("Service submitted. Booking status updated to Second Payment."));
            } else {
                sendJson(response, errorResponse("Unable to complete this booking."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void handleSubmitRating(HttpServletRequest request, HttpServletResponse response, int customerId, int bookingId) throws IOException {
        int ratingVal = parseInt(request.getParameter("rating_val"));
        String comment = safe(request.getParameter("comment"));

        if (ratingVal < 1 || ratingVal > 5) {
            sendJson(response, errorResponse("Please select rating from 1 to 5."));
            return;
        }

        if (comment.isEmpty()) {
            sendJson(response, errorResponse("Comment is required."));
            return;
        }

        try {
            boolean updated = trackingVendorDAO.submitRating(customerId, bookingId, ratingVal, comment);

            if (updated) {
                sendJson(response, successResponse("Thank you. Your rating has been submitted."));
            } else {
                sendJson(response, errorResponse("Unable to submit rating."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private String saveUploadFile(Part filePart, String prefix) throws IOException {
        String ext = getFileExtension(filePart.getSubmittedFileName()).toLowerCase();

        if (!isAllowedFileExtension(ext)) {
            throw new IOException("Only JPG, JPEG, PNG, WEBP and PDF files are allowed.");
        }

        String fileName = prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + ext;

        File sourceUploadDir = new File(RESIT_UPLOAD_PATH);

        if (!sourceUploadDir.exists()) {
            sourceUploadDir.mkdirs();
        }

        File sourceTargetFile = new File(sourceUploadDir, fileName);

        try (InputStream inputStream = filePart.getInputStream()) {
            Files.copy(inputStream, sourceTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        String runtimePath = getServletContext().getRealPath("/resit");

        if (runtimePath != null && !runtimePath.trim().isEmpty()) {
            File runtimeUploadDir = new File(runtimePath);

            if (!runtimeUploadDir.exists()) {
                runtimeUploadDir.mkdirs();
            }

            File runtimeTargetFile = new File(runtimeUploadDir, fileName);

            if (!runtimeTargetFile.getCanonicalPath().equals(sourceTargetFile.getCanonicalPath())) {
                Files.copy(sourceTargetFile.toPath(), runtimeTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        return "resit/" + fileName;
    }

    private boolean isValidFilePart(Part part) {
        return part != null && part.getSize() > 0 && part.getSubmittedFileName() != null && !part.getSubmittedFileName().trim().isEmpty();
    }

    private boolean isAllowedFileExtension(String ext) {
        return ".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext) || ".webp".equals(ext) || ".pdf".equals(ext);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf("."));
    }

    private boolean isValidMalaysiaCoordinate(double latitude, double longitude) {
        return latitude >= 0.5 && latitude <= 7.8 && longitude >= 99.0 && longitude <= 119.5;
    }

    private String getSessionRole(HttpSession session) {
        String role = "";

        if (session.getAttribute("role") != null) {
            role = session.getAttribute("role").toString();
        } else if (session.getAttribute("userRole") != null) {
            role = session.getAttribute("userRole").toString();
        } else if (session.getAttribute("accountType") != null) {
            role = session.getAttribute("accountType").toString();
        }

        return safe(role);
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

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
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

    private void sendJson(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }

    private String safeDefault(String value, String fallback) {
        String clean = safe(value);

        if (clean.isEmpty()) {
            return fallback;
        }

        return clean;
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}