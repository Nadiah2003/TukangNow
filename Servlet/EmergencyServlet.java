package Servlet;

import DAO.EmergencyDAO;
import Model.EmergencyBookingResult;
import Model.EmergencyCustomerData;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
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

@WebServlet("/EmergencyServlet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 30
)
public class EmergencyServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private EmergencyDAO emergencyDAO;
    private final Gson gson = new Gson();

    private static final String EVIDENCE_UPLOAD_PATH = "C:" + File.separator + "xampp" + File.separator + "htdocs" + File.separator + "TukangNow" + File.separator + "src" + File.separator + "main" + File.separator + "webapp" + File.separator + "evidence";

    @Override
    public void init() throws ServletException {
        emergencyDAO = new EmergencyDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "index.html")) {
            return;
        }

        try {
            int userId = Integer.parseInt(session.getAttribute("userId").toString());
            EmergencyCustomerData data = emergencyDAO.getCustomerData(userId);

            if (data == null) {
                sendJson(response, errorResponse("Customer record not found."));
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

        if (SessionUtil.isSessionExpired(session, response, "userId", "index.html")) {
            return;
        }

        String action = safe(request.getParameter("action"));

        try {
            if ("uploadEvidence".equalsIgnoreCase(action)) {
                String evidencePath = saveEvidenceImages(request);

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("message", "Evidence uploaded.");
                result.put("evidencePath", evidencePath);

                sendJson(response, gson.toJson(result));
                return;
            }

            int userId = Integer.parseInt(session.getAttribute("userId").toString());
            String category = safe(request.getParameter("category"));
            String problem = safeDefault(request.getParameter("problem"), "Emergency SOS");

            if (category.isEmpty()) {
                sendJson(response, errorResponse("Service category is required."));
                return;
            }

            EmergencyBookingResult result = emergencyDAO.createEmergencyBooking(userId, category, problem);
            sendJson(response, gson.toJson(result));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private String saveEvidenceImages(HttpServletRequest request) throws IOException, ServletException {
        File sourceUploadDir = new File(EVIDENCE_UPLOAD_PATH);

        if (!sourceUploadDir.exists()) {
            sourceUploadDir.mkdirs();
        }

        String runtimeEvidencePath = getServletContext().getRealPath("/evidence");
        File runtimeUploadDir = null;

        if (runtimeEvidencePath != null && !runtimeEvidencePath.trim().isEmpty()) {
            runtimeUploadDir = new File(runtimeEvidencePath);

            if (!runtimeUploadDir.exists()) {
                runtimeUploadDir.mkdirs();
            }
        }

        Collection<Part> parts = request.getParts();
        StringBuilder savedFiles = new StringBuilder();
        int imageCount = 0;

        for (Part part : parts) {
            if (!"evidenceImages".equals(part.getName())) {
                continue;
            }

            if (part.getSize() <= 0) {
                continue;
            }

            if (imageCount >= 3) {
                break;
            }

            String submittedFileName = safe(part.getSubmittedFileName());

            if (submittedFileName.isEmpty()) {
                continue;
            }

            String extension = getFileExtension(submittedFileName).toLowerCase();

            if (!isAllowedImageExtension(extension)) {
                throw new IOException("Only JPG, JPEG, PNG and WEBP files are allowed.");
            }

            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + "_" + imageCount + extension;
            File sourceTargetFile = new File(sourceUploadDir, fileName);

            try (InputStream inputStream = part.getInputStream()) {
                Files.copy(inputStream, sourceTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            if (runtimeUploadDir != null) {
                File runtimeTargetFile = new File(runtimeUploadDir, fileName);

                if (!runtimeTargetFile.getCanonicalPath().equals(sourceTargetFile.getCanonicalPath())) {
                    Files.copy(sourceTargetFile.toPath(), runtimeTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            if (savedFiles.length() > 0) {
                savedFiles.append(",");
            }

            savedFiles.append(fileName);
            imageCount++;
        }

        return savedFiles.toString();
    }

    private boolean isAllowedImageExtension(String extension) {
        return ".jpg".equals(extension)
                || ".jpeg".equals(extension)
                || ".png".equals(extension)
                || ".webp".equals(extension);
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

    private String errorResponse(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "error");
        map.put("message", message == null ? "Unknown error." : message);
        return gson.toJson(map);
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

    private void sendJson(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
}