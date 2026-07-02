package Servlet;

import DAO.HomeAdminDAO;
import Model.AdminHomeData;
import Model.AdminHomeVendor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/HomeAdminServlet")
public class HomeAdminServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private HomeAdminDAO homeAdminDAO;

    private static final String PROFILE_UPLOAD_PATH = File.separator + "home" + File.separator + "s72009" + File.separator + "profiles";
    private static final String LICENCE_UPLOAD_PATH = File.separator + "home" + File.separator + "s72009" + File.separator + "licences";

    @Override
    public void init() {
        homeAdminDAO = new HomeAdminDAO();
        ensureFolder(PROFILE_UPLOAD_PATH);
        ensureFolder(LICENCE_UPLOAD_PATH);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("profileImage".equals(action)) {
            serveFile(request, response, PROFILE_UPLOAD_PATH, true);
            return;
        }

        if ("licenseFile".equals(action)) {
            serveFile(request, response, LICENCE_UPLOAD_PATH, false);
            return;
        }

        setJsonResponse(response);

        int adminId = getLoginUserId(request);

        if (adminId <= 0) {
            sendJson(response, "{\"status\":\"error\",\"message\":\"Session not found. Admin login session is missing. Please login again.\"}");
            return;
        }

        try {
            AdminHomeData data = homeAdminDAO.getHomeAdminData(adminId);
            List<Map<String, Object>> notifications = homeAdminDAO.getNotifications(adminId);
            sendJson(response, buildDashboardJson(data, notifications));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, "{\"status\":\"error\",\"message\":\"Database Error: " + jsonEscape(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        int adminId = getLoginUserId(request);

        if (adminId <= 0) {
            sendJson(response, "{\"status\":\"error\",\"message\":\"Session not found. Admin login session is missing. Please login again.\"}");
            return;
        }

        doGet(request, response);
    }

    private int getLoginUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("userId") != null) {
            try {
                return Integer.parseInt(session.getAttribute("userId").toString());
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

        String requestUserId = request.getParameter("userId");

        if (requestUserId == null || requestUserId.trim().isEmpty()) {
            requestUserId = request.getParameter("adminId");
        }

        try {
            return Integer.parseInt(requestUserId);
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

    private String buildDashboardJson(AdminHomeData data, List<Map<String, Object>> notifications) {
        StringBuilder json = new StringBuilder();

        json.append("{");
        json.append("\"status\":\"success\",");
        json.append("\"adminProfile\":\"").append(jsonEscape(data.getAdminProfile())).append("\",");
        json.append("\"newApps\":").append(data.getNewApps()).append(",");
        json.append("\"activeVendors\":").append(data.getActiveVendors()).append(",");
        json.append("\"newReports\":").append(data.getNewReports()).append(",");
        json.append("\"notifications\":[");

        for (int i = 0; i < notifications.size(); i++) {
            Map<String, Object> notification = notifications.get(i);

            json.append("{");
            json.append("\"type\":\"").append(jsonEscape(String.valueOf(notification.get("type")))).append("\",");
            json.append("\"icon\":\"").append(jsonEscape(String.valueOf(notification.get("icon")))).append("\",");
            json.append("\"title\":\"").append(jsonEscape(String.valueOf(notification.get("title")))).append("\",");
            json.append("\"message\":\"").append(jsonEscape(String.valueOf(notification.get("message")))).append("\",");
            json.append("\"count\":").append(toInt(notification.get("count"))).append(",");
            json.append("\"link\":\"").append(jsonEscape(String.valueOf(notification.get("link")))).append("\"");
            json.append("}");

            if (i < notifications.size() - 1) {
                json.append(",");
            }
        }

        json.append("],");
        json.append("\"vendors\":[");

        List<AdminHomeVendor> vendors = data.getVendors();

        for (int i = 0; i < vendors.size(); i++) {
            AdminHomeVendor vendor = vendors.get(i);

            json.append("{");
            json.append("\"name\":\"").append(jsonEscape(vendor.getName())).append("\",");
            json.append("\"phone\":\"").append(jsonEscape(vendor.getPhone())).append("\",");
            json.append("\"status\":\"").append(jsonEscape(vendor.getStatus())).append("\",");
            json.append("\"profilePath\":\"").append(jsonEscape(vendor.getProfilePath())).append("\",");
            json.append("\"docUrl\":\"").append(jsonEscape(vendor.getDocUrl())).append("\",");
            json.append("\"rating\":\"").append(jsonEscape(vendor.getRating())).append("\",");
            json.append("\"workDone\":\"").append(jsonEscape(vendor.getWorkDone())).append("\"");
            json.append("}");

            if (i < vendors.size() - 1) {
                json.append(",");
            }
        }

        json.append("]");
        json.append("}");

        return json.toString();
    }

    private void serveFile(HttpServletRequest request, HttpServletResponse response, String folderPath, boolean imageOnly) throws IOException {
        int userId = getLoginUserId(request);

        if (userId <= 0) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            sendJson(response, "{\"status\":\"error\",\"message\":\"Session not found. Login session is missing.\"}");
            return;
        }

        String fileName = getOnlyFileName(request.getParameter("file"));

        if (fileName == null || fileName.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String extension = getFileExtension(fileName);

        if (imageOnly && !isAllowedImageExtension(extension)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!imageOnly && !isAllowedDocumentExtension(extension)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        File file = new File(folderPath, fileName);

        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(getMimeType(extension));
        response.setContentLengthLong(file.length());
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Content-Disposition", "inline; filename=\"" + fileName.replace("\"", "") + "\"");

        try (FileInputStream inputStream = new FileInputStream(file);
             OutputStream outputStream = response.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
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

    private boolean isAllowedDocumentExtension(String extension) {
        return "jpg".equals(extension)
                || "jpeg".equals(extension)
                || "png".equals(extension)
                || "gif".equals(extension)
                || "webp".equals(extension)
                || "pdf".equals(extension);
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

        if ("pdf".equals(extension)) {
            return "application/pdf";
        }

        return "application/octet-stream";
    }

    private int toInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private void sendJson(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}