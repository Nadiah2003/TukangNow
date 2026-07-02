package Servlet;

import DAO.ProfileCustomerDAO;
import Model.ProfileCustomerData;
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
import Servlet.SessionUtil;

@WebServlet("/ProfileCustomerServlet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 20
)
public class ProfileCustomerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private ProfileCustomerDAO profileCustomerDAO;
    private final Gson gson = new Gson();

    private static final String PROFILE_UPLOAD_PATH = File.separator + "home" + File.separator + "s72009" + File.separator + "profiles";

    @Override
    public void init() {
        profileCustomerDAO = new ProfileCustomerDAO();

        File profileDir = new File(PROFILE_UPLOAD_PATH);

        if (!profileDir.exists()) {
            profileDir.mkdirs();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "index.html")) {
            return;
        }

        String action = request.getParameter("action");

        try {
            if (action == null || action.trim().isEmpty() || "get".equalsIgnoreCase(action)) {
                int customerId = Integer.parseInt(session.getAttribute("userId").toString());
                ProfileCustomerData data = profileCustomerDAO.getProfile(customerId);

                if (data == null) {
                    sendJson(response, errorResponse("User not found."));
                    return;
                }

                data.setStatus("success");

                if (data.getProfile_path() == null || data.getProfile_path().trim().isEmpty()) {
                    data.setProfile_path("");
                }

                sendJson(response, gson.toJson(data));
                return;
            }

            sendJson(response, errorResponse("Invalid action."));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "index.html")) {
            return;
        }

        String action = request.getParameter("action");

        try {
            int customerId = Integer.parseInt(session.getAttribute("userId").toString());

            if ("updateProfile".equalsIgnoreCase(action)) {
                updateProfile(request, response, customerId);
                return;
            }

            if ("uploadImage".equalsIgnoreCase(action)) {
                uploadImage(request, response, customerId);
                return;
            }

            if ("changePassword".equalsIgnoreCase(action)) {
                changePassword(request, response, customerId);
                return;
            }

            if ("logout".equalsIgnoreCase(action)) {
                session.invalidate();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("message", "Logout successful.");
                result.put("redirect", "index.html");

                sendJson(response, gson.toJson(result));
                return;
            }

            sendJson(response, errorResponse("Invalid action."));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void updateProfile(HttpServletRequest request, HttpServletResponse response, int customerId) throws IOException {
        String name = safe(request.getParameter("name"));
        String email = safe(request.getParameter("email"));
        String phone = safe(request.getParameter("phone"));
        String address = safe(request.getParameter("address"));
        String postcode = safe(request.getParameter("postcode"));
        String city = safe(request.getParameter("city"));
        String state = safe(request.getParameter("state"));
        String country = safe(request.getParameter("country"));

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            sendJson(response, errorResponse("Name, email and phone cannot be empty."));
            return;
        }

        try {
            boolean updated = profileCustomerDAO.updateProfile(customerId, name, email, phone, address, postcode, city, state, country);

            if (updated) {
                sendJson(response, successResponse("Profile successfully updated!"));
            } else {
                sendJson(response, errorResponse("Update failed."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void uploadImage(HttpServletRequest request, HttpServletResponse response, int customerId) throws IOException, ServletException {
        Part filePart = request.getPart("profilePic");

        if (filePart == null || filePart.getSize() <= 0 || filePart.getSubmittedFileName() == null || filePart.getSubmittedFileName().trim().isEmpty()) {
            sendJson(response, errorResponse("No image selected."));
            return;
        }

        String ext = getFileExtension(filePart.getSubmittedFileName()).toLowerCase();

        if (!isAllowedImageExtension(ext)) {
            sendJson(response, errorResponse("Only JPG, JPEG, PNG and WEBP files are allowed."));
            return;
        }

        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + ext;

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
            boolean updated = profileCustomerDAO.updateProfileImage(customerId, fileName);

            if (updated) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("profile_path", fileName);
                result.put("message", "Image uploaded!");
                sendJson(response, gson.toJson(result));
            } else {
                sendJson(response, errorResponse("Failed to update image."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void changePassword(HttpServletRequest request, HttpServletResponse response, int customerId) throws IOException {
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
            boolean correctOldPassword = profileCustomerDAO.isOldPasswordCorrect(customerId, oldPass);

            if (!correctOldPassword) {
                sendJson(response, errorResponse("Old password incorrect."));
                return;
            }

            boolean updated = profileCustomerDAO.changePassword(customerId, newPass);

            if (updated) {
                sendJson(response, successResponse("Password updated!"));
            } else {
                sendJson(response, errorResponse("Password update failed."));
            }
        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 8
                && password.matches(".*[A-Z].*")
                && password.matches(".*[0-9].*")
                && password.matches(".*[@#$%^&*].*");
    }

    private boolean isAllowedImageExtension(String ext) {
        return ".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext) || ".webp".equals(ext);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf("."));
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

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}