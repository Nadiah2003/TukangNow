package Servlet;

import DAO.HomeVendorDAO;
import Model.HomeVendorData;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/HomeVendorServlet")
public class HomeVendorServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private HomeVendorDAO homeVendorDAO;
    private final Gson gson = new Gson();

    private static final String PROFILE_UPLOAD_PATH = File.separator + "home" + File.separator + "s72009" + File.separator + "profiles";

    @Override
    public void init() {
        homeVendorDAO = new HomeVendorDAO();
        ensureFolder(PROFILE_UPLOAD_PATH);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("profileImage".equals(action)) {
            serveProfileImage(request, response);
            return;
        }

        setJsonResponse(response);

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            sendJson(response, sessionExpiredResponse());
            return;
        }

        Integer vendorId = getSessionUserId(request);

        if (vendorId == null) {
            sendJson(response, sessionExpiredResponse());
            return;
        }

        try {
            HomeVendorData data = homeVendorDAO.getDashboardData(vendorId);
            JsonObject result = gson.toJsonTree(data).getAsJsonObject();
            result.add("notifications", gson.toJsonTree(homeVendorDAO.getVendorNotifications(vendorId)));
            result.addProperty("unreadNotificationsCount", homeVendorDAO.getUnreadNotificationCount(vendorId));
            sendJson(response, gson.toJson(result));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            sendJson(response, sessionExpiredResponse());
            return;
        }

        Integer vendorId = getSessionUserId(request);

        if (vendorId == null) {
            sendJson(response, sessionExpiredResponse());
            return;
        }

        try {
            JsonObject jsonObject = gson.fromJson(request.getReader(), JsonObject.class);

            if (jsonObject == null || !jsonObject.has("action")) {
                sendJson(response, errorResponse("Invalid request."));
                return;
            }

            String action = jsonObject.get("action").getAsString();

            if ("respondNow".equals(action)) {
                if (!jsonObject.has("bookingId")) {
                    sendJson(response, errorResponse("Invalid Booking ID."));
                    return;
                }

                int bookingId = jsonObject.get("bookingId").getAsInt();

                if (bookingId <= 0) {
                    sendJson(response, errorResponse("Invalid Booking ID."));
                    return;
                }

                String resultMessage = homeVendorDAO.respondNow(vendorId, bookingId);

                if ("success".equals(resultMessage)) {
                    sendJson(response, successResponse("Booking successfully accepted!"));
                } else {
                    sendJson(response, errorResponse(resultMessage));
                }

                return;
            }

            if ("rejectBooking".equals(action)) {
                if (!jsonObject.has("bookingId")) {
                    sendJson(response, errorResponse("Invalid Booking ID."));
                    return;
                }

                int bookingId = jsonObject.get("bookingId").getAsInt();

                if (bookingId <= 0) {
                    sendJson(response, errorResponse("Invalid Booking ID."));
                    return;
                }

                String resultMessage = homeVendorDAO.rejectBooking(vendorId, bookingId);

                if ("success".equals(resultMessage)) {
                    sendJson(response, successResponse("Booking successfully rejected."));
                } else {
                    sendJson(response, errorResponse(resultMessage));
                }

                return;
            }

            sendJson(response, errorResponse("Invalid action."));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse(e.getMessage()));
        }
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

    private void serveProfileImage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            sendJson(response, sessionExpiredResponse());
            return;
        }

        String fileName = getOnlyFileName(request.getParameter("file"));

        if (fileName == null || fileName.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String extension = getFileExtension(fileName);

        if (!isAllowedImageExtension(extension)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        File file = new File(PROFILE_UPLOAD_PATH, fileName);

        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(getMimeType(extension));
        response.setContentLengthLong(file.length());
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

        try (FileInputStream inputStream = new FileInputStream(file);
             OutputStream outputStream = response.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private void ensureFolder(String folderPath) {
        File folder = new File(folderPath);

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    private String getOnlyFileName(String path) {
        if (path == null) {
            return "";
        }

        String cleanPath = path.replace("\\", "/");

        if (cleanPath.contains("/")) {
            cleanPath = cleanPath.substring(cleanPath.lastIndexOf("/") + 1);
        }

        return cleanPath.replace("..", "").trim();
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private boolean isAllowedImageExtension(String extension) {
        return "jpg".equals(extension)
                || "jpeg".equals(extension)
                || "png".equals(extension)
                || "gif".equals(extension)
                || "webp".equals(extension);
    }

    private String getMimeType(String extension) {
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return "image/jpeg";
        }

        if ("png".equals(extension)) {
            return "image/png";
        }

        if ("gif".equals(extension)) {
            return "image/gif";
        }

        if ("webp".equals(extension)) {
            return "image/webp";
        }

        return "application/octet-stream";
    }

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
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
        map.put("message", message == null ? "Something went wrong." : message);
        return gson.toJson(map);
    }

    private String sessionExpiredResponse() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("status", "session_expired");
        map.put("message", "Session not found. Login session is missing.");
        map.put("redirect", "");
        return gson.toJson(map);
    }

    private void sendJson(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
}