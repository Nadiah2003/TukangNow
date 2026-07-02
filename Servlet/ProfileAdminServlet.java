package Servlet;

import DAO.ProfileAdminDAO;
import Model.AdminProfile;
import Model.AdminUser;
import Model.EventData;
import Model.EstimateItem;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

@WebServlet("/ProfileAdminServlet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 40
)
public class ProfileAdminServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private ProfileAdminDAO profileAdminDAO;
    private final Gson gson = new Gson();

    private static final String PROFILE_UPLOAD_PATH = File.separator + "home" + File.separator + "s72009" + File.separator + "profiles";
    private static final String EVENT_UPLOAD_PATH = File.separator + "home" + File.separator + "s72009" + File.separator + "events";
    private static final String RESIT_UPLOAD_PATH = File.separator + "home" + File.separator + "s72009" + File.separator + "resit";
    private static final String EVIDENCE_UPLOAD_PATH = File.separator + "home" + File.separator + "s72009" + File.separator + "evidence";
    private static final String LICENCE_UPLOAD_PATH = File.separator + "home" + File.separator + "s72009" + File.separator + "licences";

    @Override
    public void init() {
        profileAdminDAO = new ProfileAdminDAO();
        ensureUploadFolders();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        String action = request.getParameter("action");
        int adminId = getLoginAdminId(request);

        if (adminId <= 0) {
            response.setContentType("application/json");
            sendJson(response, errorResponse("Session expired."));
            return;
        }

        try {
            profileAdminDAO.updateExpiredEvents();

            if ("get".equalsIgnoreCase(action)) {
                response.setContentType("application/json");
                AdminProfile admin = profileAdminDAO.getAdminProfile(adminId);

                if (admin == null) {
                    sendJson(response, errorResponse("Admin not found."));
                    return;
                }

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("id", admin.getId());
                result.put("name", admin.getName());
                result.put("email", admin.getEmail());
                result.put("admin_level", admin.getAdmin_level());
                result.put("profile_path", admin.getProfile_path() == null ? "" : admin.getProfile_path());

                sendJson(response, gson.toJson(result));
                return;
            }

            if ("admins".equalsIgnoreCase(action)) {
                response.setContentType("application/json");
                int currentLevel = profileAdminDAO.getAdminLevel(adminId);

                if (currentLevel != 1) {
                    sendJson(response, errorResponse("Only Leader Admin can view admin list."));
                    return;
                }

                List<AdminUser> admins = profileAdminDAO.getOtherAdmins(adminId);

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("admins", admins);

                sendJson(response, gson.toJson(result));
                return;
            }

            if ("estimateItems".equalsIgnoreCase(action)) {
                response.setContentType("application/json");
                int currentLevel = profileAdminDAO.getAdminLevel(adminId);

                if (currentLevel != 3) {
                    sendJson(response, errorResponse("Only Estimate Admin can manage estimate items."));
                    return;
                }

                List<EstimateItem> items = profileAdminDAO.getEstimateItems();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("items", items);

                sendJson(response, gson.toJson(result));
                return;
            }

            if ("bookingMaterials".equalsIgnoreCase(action)) {
                response.setContentType("application/json");
                int currentLevel = profileAdminDAO.getAdminLevel(adminId);

                if (currentLevel != 3) {
                    sendJson(response, errorResponse("Only Estimate Admin can view booking material receipts."));
                    return;
                }

                List<Map<String, Object>> materials = profileAdminDAO.getBookingMaterialItems();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("materials", materials);

                sendJson(response, gson.toJson(result));
                return;
            }

            if ("receiptImage".equalsIgnoreCase(action)) {
                int currentLevel = profileAdminDAO.getAdminLevel(adminId);

                if (currentLevel != 3) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                serveReceiptImage(request, response);
                return;
            }

            response.setContentType("application/json");
            sendJson(response, errorResponse("Invalid action."));

        } catch (Exception e) {
            response.setContentType("application/json");
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        int adminId = getLoginAdminId(request);

        if (adminId <= 0) {
            sendJson(response, errorResponse("Session expired."));
            return;
        }

        String action = request.getParameter("action");

        try {
            profileAdminDAO.updateExpiredEvents();

            if ("updateProfile".equalsIgnoreCase(action)) {
                updateProfile(request, response, adminId);
                return;
            }

            if ("uploadImage".equalsIgnoreCase(action)) {
                uploadImage(request, response, adminId);
                return;
            }

            if ("changePassword".equalsIgnoreCase(action)) {
                changePassword(request, response, adminId);
                return;
            }

            if ("updateRole".equalsIgnoreCase(action)) {
                updateRole(request, response, adminId);
                return;
            }

            if ("addEvent".equalsIgnoreCase(action)) {
                addEvent(request, response, adminId);
                return;
            }

            if ("addEstimate".equalsIgnoreCase(action)) {
                addEstimate(request, response, adminId);
                return;
            }

            if ("updateEstimate".equalsIgnoreCase(action)) {
                updateEstimate(request, response, adminId);
                return;
            }

            if ("deleteEstimate".equalsIgnoreCase(action)) {
                deleteEstimate(request, response, adminId);
                return;
            }

            sendJson(response, errorResponse("Invalid action."));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private int getLoginAdminId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("userId") != null) {
            try {
                return Integer.parseInt(session.getAttribute("userId").toString());
            } catch (Exception e) {
                return 0;
            }
        }

        if (session != null && session.getAttribute("adminId") != null) {
            try {
                return Integer.parseInt(session.getAttribute("adminId").toString());
            } catch (Exception e) {
                return 0;
            }
        }

        String cookieAdminId = getCookieValue(request, "tn_adminId");

        if (!cookieAdminId.trim().isEmpty()) {
            try {
                return Integer.parseInt(cookieAdminId.trim());
            } catch (Exception e) {
                return 0;
            }
        }

        String cookieUserId = getCookieValue(request, "tn_userId");

        if (!cookieUserId.trim().isEmpty()) {
            try {
                return Integer.parseInt(cookieUserId.trim());
            } catch (Exception e) {
                return 0;
            }
        }

        String requestAdminId = request.getParameter("adminId");

        if (requestAdminId == null || requestAdminId.trim().isEmpty()) {
            requestAdminId = request.getParameter("userId");
        }

        try {
            return Integer.parseInt(requestAdminId);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return "";
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                try {
                    return URLDecoder.decode(cookie.getValue(), "UTF-8");
                } catch (Exception e) {
                    return cookie.getValue();
                }
            }
        }

        return "";
    }

    private void updateProfile(HttpServletRequest request, HttpServletResponse response, int adminId) throws IOException {
        String name = safe(request.getParameter("name"));
        String email = safe(request.getParameter("email"));

        if (name.isEmpty() || email.isEmpty()) {
            sendJson(response, errorResponse("Name and Email cannot be empty."));
            return;
        }

        try {
            boolean updated = profileAdminDAO.updateProfile(adminId, name, email);

            if (updated) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("message", "Profile updated successfully.");
                sendJson(response, gson.toJson(result));
            } else {
                sendJson(response, errorResponse("Failed to update profile."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void uploadImage(HttpServletRequest request, HttpServletResponse response, int adminId) throws IOException, ServletException {
        Part filePart = request.getPart("profilePic");

        if (filePart == null || filePart.getSize() <= 0 || filePart.getSubmittedFileName() == null || filePart.getSubmittedFileName().trim().isEmpty()) {
            sendJson(response, errorResponse("No file uploaded or upload error."));
            return;
        }

        String ext = getFileExtension(filePart.getSubmittedFileName()).toLowerCase();

        if (!isAllowedImageExtension(ext)) {
            sendJson(response, errorResponse("Only JPG, JPEG, PNG and WEBP files are allowed."));
            return;
        }

        String fileName = buildFileName(ext);
        File sourceUploadDir = new File(PROFILE_UPLOAD_PATH);

        if (!sourceUploadDir.exists()) {
            sourceUploadDir.mkdirs();
        }

        File sourceTargetFile = new File(sourceUploadDir, fileName);

        try (InputStream inputStream = filePart.getInputStream()) {
            Files.copy(inputStream, sourceTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        String runtimeProfilePath = getServletContext().getRealPath("/profiles");

        if (runtimeProfilePath != null && !runtimeProfilePath.trim().isEmpty()) {
            File runtimeUploadDir = new File(runtimeProfilePath);

            if (!runtimeUploadDir.exists()) {
                runtimeUploadDir.mkdirs();
            }

            File runtimeTargetFile = new File(runtimeUploadDir, fileName);

            if (!runtimeTargetFile.getCanonicalPath().equals(sourceTargetFile.getCanonicalPath())) {
                Files.copy(sourceTargetFile.toPath(), runtimeTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        try {
            boolean updated = profileAdminDAO.updateProfileImage(adminId, fileName);

            if (updated) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("message", "Profile picture uploaded successfully.");
                result.put("path", fileName);
                result.put("fileName", fileName);
                sendJson(response, gson.toJson(result));
            } else {
                sendJson(response, errorResponse("Failed to update database."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void changePassword(HttpServletRequest request, HttpServletResponse response, int adminId) throws IOException {
        String oldPass = safe(request.getParameter("oldPass"));
        String newPass = safe(request.getParameter("newPass"));

        if (oldPass.isEmpty() || newPass.isEmpty()) {
            sendJson(response, errorResponse("Password cannot be empty."));
            return;
        }

        if (!isPasswordValid(newPass)) {
            sendJson(response, errorResponse("New password does not meet the requirements."));
            return;
        }

        try {
            boolean correct = profileAdminDAO.isOldPasswordCorrect(adminId, oldPass);

            if (!correct) {
                sendJson(response, errorResponse("Current password is incorrect."));
                return;
            }

            boolean updated = profileAdminDAO.changePassword(adminId, newPass);

            if (updated) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("message", "Password updated successfully.");
                sendJson(response, gson.toJson(result));
            } else {
                sendJson(response, errorResponse("Failed to update password."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void updateRole(HttpServletRequest request, HttpServletResponse response, int adminId) throws IOException {
        int targetAdminId = parseInt(request.getParameter("admin_id"));
        int adminLevel = parseInt(request.getParameter("admin_level"));

        if (targetAdminId <= 0) {
            sendJson(response, errorResponse("Invalid admin."));
            return;
        }

        if (targetAdminId == adminId) {
            sendJson(response, errorResponse("You cannot change your own role."));
            return;
        }

        if (adminLevel != 0 && adminLevel != 1 && adminLevel != 2 && adminLevel != 3) {
            sendJson(response, errorResponse("Invalid role selected."));
            return;
        }

        try {
            int currentLevel = profileAdminDAO.getAdminLevel(adminId);

            if (currentLevel != 1) {
                sendJson(response, errorResponse("Only Leader Admin can change admin role."));
                return;
            }

            boolean updated = profileAdminDAO.updateAdminRole(targetAdminId, adminLevel);

            if (updated) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("message", "Admin role updated successfully.");
                sendJson(response, gson.toJson(result));
            } else {
                sendJson(response, errorResponse("Failed to update admin role."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void addEvent(HttpServletRequest request, HttpServletResponse response, int adminId) throws IOException, ServletException {
        try {
            int currentLevel = profileAdminDAO.getAdminLevel(adminId);

            if (currentLevel != 2) {
                sendJson(response, errorResponse("Only Event Admin can add event."));
                return;
            }

            String title = safe(request.getParameter("title"));
            String description = safe(request.getParameter("description"));
            String discountCode = safe(request.getParameter("discount_code")).toUpperCase();
            double discountPercentage = parseDouble(request.getParameter("discount_percentage"));
            String startDate = safe(request.getParameter("start_date"));
            String endDate = safe(request.getParameter("end_date"));

            if (title.isEmpty() || description.isEmpty() || discountCode.isEmpty() || startDate.isEmpty() || endDate.isEmpty()) {
                sendJson(response, errorResponse("Please complete all event fields."));
                return;
            }

            if (discountPercentage < 0 || discountPercentage > 100) {
                sendJson(response, errorResponse("Discount percentage must be between 0 and 100."));
                return;
            }

            String imagePath = saveEventImage(request);

            EventData eventData = new EventData();
            eventData.setTitle(title);
            eventData.setDescription(description);
            eventData.setImage_path(imagePath);
            eventData.setDiscount_code(discountCode);
            eventData.setDiscount_percentage(discountPercentage);
            eventData.setStart_date(startDate);
            eventData.setEnd_date(endDate);
            eventData.setStatus("active");
            eventData.setAdmin_id(adminId);

            boolean inserted = profileAdminDAO.insertEvent(eventData);

            if (inserted) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("message", "Event added successfully.");
                result.put("image_path", imagePath);
                result.put("event_status", "active");
                sendJson(response, gson.toJson(result));
            } else {
                sendJson(response, errorResponse("Failed to add event."));
            }

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void addEstimate(HttpServletRequest request, HttpServletResponse response, int adminId) throws IOException {
        try {
            int currentLevel = profileAdminDAO.getAdminLevel(adminId);

            if (currentLevel != 3) {
                sendJson(response, errorResponse("Only Estimate Admin can manage estimate items."));
                return;
            }

            EstimateItem item = buildEstimateItem(request, false);

            if (item.getServiceKeyword().isEmpty() || item.getItemName().isEmpty() || item.getItemPrice() < 0) {
                sendJson(response, errorResponse("Please complete all estimate fields."));
                return;
            }

            boolean inserted = profileAdminDAO.insertEstimateItem(item);

            if (inserted) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("message", "Estimate item added successfully.");
                sendJson(response, gson.toJson(result));
            } else {
                sendJson(response, errorResponse("Failed to add estimate item."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void updateEstimate(HttpServletRequest request, HttpServletResponse response, int adminId) throws IOException {
        try {
            int currentLevel = profileAdminDAO.getAdminLevel(adminId);

            if (currentLevel != 3) {
                sendJson(response, errorResponse("Only Estimate Admin can manage estimate items."));
                return;
            }

            EstimateItem item = buildEstimateItem(request, true);

            if (item.getId() <= 0 || item.getServiceKeyword().isEmpty() || item.getItemName().isEmpty() || item.getItemPrice() < 0) {
                sendJson(response, errorResponse("Please complete all estimate fields."));
                return;
            }

            boolean updated = profileAdminDAO.updateEstimateItem(item);

            if (updated) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("message", "Estimate item updated successfully.");
                sendJson(response, gson.toJson(result));
            } else {
                sendJson(response, errorResponse("Failed to update estimate item."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void deleteEstimate(HttpServletRequest request, HttpServletResponse response, int adminId) throws IOException {
        try {
            int currentLevel = profileAdminDAO.getAdminLevel(adminId);

            if (currentLevel != 3) {
                sendJson(response, errorResponse("Only Estimate Admin can manage estimate items."));
                return;
            }

            int estimateId = parseInt(request.getParameter("id"));

            if (estimateId <= 0) {
                sendJson(response, errorResponse("Invalid estimate item."));
                return;
            }

            boolean deleted = profileAdminDAO.deleteEstimateItem(estimateId);

            if (deleted) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("message", "Estimate item deleted successfully.");
                sendJson(response, gson.toJson(result));
            } else {
                sendJson(response, errorResponse("Failed to delete estimate item."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private EstimateItem buildEstimateItem(HttpServletRequest request, boolean includeId) {
        EstimateItem item = new EstimateItem();

        if (includeId) {
            item.setId(parseInt(request.getParameter("id")));
        }

        item.setServiceKeyword(safe(request.getParameter("service_keyword")));
        item.setItemName(safe(request.getParameter("item_name")));
        item.setItemPrice(parseDouble(request.getParameter("item_price")));

        return item;
    }

    private String saveEventImage(HttpServletRequest request) throws IOException, ServletException {
        Part filePart = request.getPart("eventImage");

        if (filePart == null || filePart.getSize() <= 0 || filePart.getSubmittedFileName() == null || filePart.getSubmittedFileName().trim().isEmpty()) {
            return "";
        }

        String ext = getFileExtension(filePart.getSubmittedFileName()).toLowerCase();

        if (!isAllowedImageExtension(ext)) {
            throw new ServletException("Only JPG, JPEG, PNG and WEBP files are allowed.");
        }

        String fileName = buildFileName(ext);
        File sourceUploadDir = new File(EVENT_UPLOAD_PATH);

        if (!sourceUploadDir.exists()) {
            sourceUploadDir.mkdirs();
        }

        File sourceTargetFile = new File(sourceUploadDir, fileName);

        try (InputStream inputStream = filePart.getInputStream()) {
            Files.copy(inputStream, sourceTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        String runtimeEventPath = getServletContext().getRealPath("/events");

        if (runtimeEventPath != null && !runtimeEventPath.trim().isEmpty()) {
            File runtimeUploadDir = new File(runtimeEventPath);

            if (!runtimeUploadDir.exists()) {
                runtimeUploadDir.mkdirs();
            }

            File runtimeTargetFile = new File(runtimeUploadDir, fileName);

            if (!runtimeTargetFile.getCanonicalPath().equals(sourceTargetFile.getCanonicalPath())) {
                Files.copy(sourceTargetFile.toPath(), runtimeTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        return fileName;
    }

    private void serveReceiptImage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fileParam = safe(request.getParameter("file"));

        if (fileParam.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String fileName = getOnlyFileName(fileParam);
        String ext = getFileExtension(fileName).toLowerCase();

        if (!isAllowedReceiptExtension(ext)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        File baseDir = new File(RESIT_UPLOAD_PATH).getCanonicalFile();
        File targetFile = new File(baseDir, fileName).getCanonicalFile();

        if (!targetFile.getPath().startsWith(baseDir.getPath() + File.separator)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!targetFile.exists() || !targetFile.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(getMimeType(ext));
        response.setHeader("Content-Disposition", "inline; filename=\"" + fileName.replace("\"", "") + "\"");
        response.setContentLengthLong(targetFile.length());

        try (OutputStream outputStream = response.getOutputStream()) {
            Files.copy(targetFile.toPath(), outputStream);
            outputStream.flush();
        }
    }

    private void ensureUploadFolders() {
        new File(PROFILE_UPLOAD_PATH).mkdirs();
        new File(EVENT_UPLOAD_PATH).mkdirs();
        new File(RESIT_UPLOAD_PATH).mkdirs();
        new File(EVIDENCE_UPLOAD_PATH).mkdirs();
        new File(LICENCE_UPLOAD_PATH).mkdirs();
    }

    private String getOnlyFileName(String path) {
        if (path == null) {
            return "";
        }

        String normalized = path.replace("\\", "/");
        int slashIndex = normalized.lastIndexOf("/");

        if (slashIndex >= 0) {
            return normalized.substring(slashIndex + 1);
        }

        return normalized;
    }

    private String buildFileName(String ext) {
        return System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + ext;
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[0-9].*") &&
                password.matches(".*[@#$%^&*].*");
    }

    private boolean isAllowedImageExtension(String ext) {
        return ".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext) || ".webp".equals(ext);
    }

    private boolean isAllowedReceiptExtension(String ext) {
        return ".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext) || ".webp".equals(ext) || ".pdf".equals(ext);
    }

    private String getMimeType(String ext) {
        if (".jpg".equals(ext) || ".jpeg".equals(ext)) {
            return "image/jpeg";
        }

        if (".png".equals(ext)) {
            return "image/png";
        }

        if (".webp".equals(ext)) {
            return "image/webp";
        }

        if (".pdf".equals(ext)) {
            return "application/pdf";
        }

        return "application/octet-stream";
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf("."));
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