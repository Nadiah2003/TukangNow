package Servlet;

import DAO.ProfileVendorDAO;
import Model.ProfileVendorData;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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

@WebServlet("/ProfileVendorServlet")
@MultipartConfig
public class ProfileVendorServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String PROFILE_UPLOAD_PATH = File.separator + "home" + File.separator + "s72009" + File.separator + "profiles";
    private ProfileVendorDAO profileVendorDAO;
    private final Gson gson = new Gson();

    public void init() {
        profileVendorDAO = new ProfileVendorDAO();

        File profileDir = new File(PROFILE_UPLOAD_PATH);

        if (!profileDir.exists()) {
            profileDir.mkdirs();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        Integer vendorId = getSessionUserId(request);

        if (vendorId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            sendJson(response, errorResponse("Session expired."));
            return;
        }

        try {
            ProfileVendorData data = profileVendorDAO.getProfileData(vendorId);
            sendJson(response, gson.toJson(data));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        Integer vendorId = getSessionUserId(request);

        if (vendorId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            sendJson(response, errorResponse("Session expired."));
            return;
        }

        String action = request.getParameter("action");

        if (action == null || action.trim().isEmpty()) {
            sendJson(response, errorResponse("Invalid action."));
            return;
        }

        try {
            boolean success = false;

            if ("updateProfileInfo".equals(action)) {
                success = profileVendorDAO.updateProfileInfo(
                        vendorId,
                        request.getParameter("name"),
                        request.getParameter("email"),
                        request.getParameter("phone"),
                        request.getParameter("address"),
                        request.getParameter("postcode"),
                        request.getParameter("city"),
                        request.getParameter("state")
                );
            } else if ("updateService".equals(action)) {
                double price = parseDouble(request.getParameter("price"));
                success = profileVendorDAO.updateService(
                        vendorId,
                        price,
                        request.getParameter("subservices")
                );
            } else if ("updateAvailability".equals(action)) {
                success = profileVendorDAO.updateAvailability(
                        vendorId,
                        request.getParameter("days"),
                        request.getParameter("time")
                );
            } else if ("updatePassword".equals(action)) {
                String oldPass = request.getParameter("oldPass");
                String newPass = request.getParameter("newPass");
                String result = profileVendorDAO.updatePassword(vendorId, oldPass, newPass);

                if ("success".equals(result)) {
                    sendJson(response, successResponse("Password updated successfully."));
                } else {
                    sendJson(response, errorResponse(result));
                }

                return;
            } else if ("uploadImage".equals(action)) {
                success = uploadProfileImage(request, vendorId);
            } else {
                sendJson(response, errorResponse("Invalid action."));
                return;
            }

            if (success || "updateService".equals(action) || "updateProfileInfo".equals(action) || "updateAvailability".equals(action)) {
                profileVendorDAO.checkAndDisableFirstLogin(vendorId);
                sendJson(response, successResponse("Updated successfully."));
            } else {
                sendJson(response, errorResponse("Could not update database."));
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private boolean uploadProfileImage(HttpServletRequest request, int vendorId) throws Exception {
        Part filePart = request.getPart("profilePic");

        if (filePart == null || filePart.getSize() <= 0) {
            return false;
        }

        File uploadDir = new File(PROFILE_UPLOAD_PATH);

        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String submittedFileName = getSubmittedFileName(filePart);
        String extension = "";

        if (submittedFileName != null && submittedFileName.contains(".")) {
            extension = submittedFileName.substring(submittedFileName.lastIndexOf(".")).toLowerCase();
        }

        if (!isAllowedImageExtension(extension)) {
            throw new IOException("Only JPG, JPEG, PNG and WEBP files are allowed.");
        }

        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + extension;
        File savedFile = new File(uploadDir, fileName);

        try (InputStream inputStream = filePart.getInputStream()) {
            Files.copy(inputStream, savedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        String runtimeProfilePath = getServletContext().getRealPath("/profiles");

        if (runtimeProfilePath != null && !runtimeProfilePath.trim().isEmpty()) {
            File runtimeUploadDir = new File(runtimeProfilePath);

            if (!runtimeUploadDir.exists()) {
                runtimeUploadDir.mkdirs();
            }

            File runtimeTargetFile = new File(runtimeUploadDir, fileName);

            if (!runtimeTargetFile.getCanonicalPath().equals(savedFile.getCanonicalPath())) {
                Files.copy(savedFile.toPath(), runtimeTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        return profileVendorDAO.updateProfileImage(vendorId, fileName);
    }

    private boolean isAllowedImageExtension(String ext) {
        return ".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext) || ".webp".equals(ext);
    }

    private String getSubmittedFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");

        if (contentDisposition == null) {
            return "";
        }

        String[] items = contentDisposition.split(";");

        for (String item : items) {
            if (item.trim().startsWith("filename")) {
                return item.substring(item.indexOf("=") + 2, item.length() - 1);
            }
        }

        return "";
    }

    private Integer getSessionUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            return null;
        }

        try {
            return Integer.parseInt(session.getAttribute("userId").toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private String successResponse(String message) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("status", "success");
        map.put("message", message);
        return gson.toJson(map);
    }

    private String errorResponse(String message) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("status", "error");
        map.put("message", message);
        return gson.toJson(map);
    }

    private void sendJson(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
}