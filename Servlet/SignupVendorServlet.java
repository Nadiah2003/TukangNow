package Servlet;

import DAO.VendorDAO;
import Model.Vendor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet("/SignupVendorServlet")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,
    maxFileSize = 1024 * 1024 * 10,
    maxRequestSize = 1024 * 1024 * 50
)
public class SignupVendorServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private VendorDAO vendorDAO;

    private static final String LICENCE_UPLOAD_PATH = File.separator + "home" + File.separator + "s72009" + File.separator + "licences";
    private static final String PROFILE_UPLOAD_PATH = File.separator + "home" + File.separator + "s72009" + File.separator + "profiles";

    public void init() {
        vendorDAO = new VendorDAO();

        File licenceDir = new File(LICENCE_UPLOAD_PATH);
        File profileDir = new File(PROFILE_UPLOAD_PATH);

        if (!licenceDir.exists()) {
            licenceDir.mkdirs();
        }

        if (!profileDir.exists()) {
            profileDir.mkdirs();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phone = request.getParameter("nophone");
        String password = request.getParameter("password");
        String address = request.getParameter("address");
        String postcode = request.getParameter("postcode");
        String city = request.getParameter("city");
        String state = request.getParameter("state");

        double latitude = 0.0;
        double longitude = 0.0;

        try {
            if (request.getParameter("latitude") != null && !request.getParameter("latitude").isEmpty()) {
                latitude = Double.parseDouble(request.getParameter("latitude"));
            }

            if (request.getParameter("longitude") != null && !request.getParameter("longitude").isEmpty()) {
                longitude = Double.parseDouble(request.getParameter("longitude"));
            }
        } catch (NumberFormatException e) {
            latitude = 0.0;
            longitude = 0.0;
        }

        String cleanNum = phone != null ? phone.replaceAll("[^0-9]", "") : "";
        String phoneFormatted;

        if (!cleanNum.startsWith("60")) {
            if (cleanNum.startsWith("0")) {
                phoneFormatted = "60" + cleanNum.substring(1);
            } else {
                phoneFormatted = "60" + cleanNum;
            }
        } else {
            phoneFormatted = cleanNum;
        }

        try {
            if (vendorDAO.checkAvailability(email, phoneFormatted)) {
                sendJson(response, "error", "Email or Phone Number already registered!");
                return;
            }

            File licenceDir = new File(LICENCE_UPLOAD_PATH);
            File profileDir = new File(PROFILE_UPLOAD_PATH);

            if (!licenceDir.exists()) {
                licenceDir.mkdirs();
            }

            if (!profileDir.exists()) {
                profileDir.mkdirs();
            }

            File runtimeProfileDir = getRuntimeDir("profiles");
            File runtimeLicenceDir = getRuntimeDir("licences");

            String profileFileName = "";
            Part profilePart = request.getPart("profileImage");

            if (profilePart != null && profilePart.getSize() > 0 && profilePart.getSubmittedFileName() != null && !profilePart.getSubmittedFileName().isEmpty()) {
                String submittedProfileName = sanitizeFileName(profilePart.getSubmittedFileName());
                String ext = getFileExtension(submittedProfileName);
                profileFileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + ext;

                File profileFile = new File(profileDir, profileFileName);

                try (InputStream inputStream = profilePart.getInputStream()) {
                    Files.copy(inputStream, profileFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                if (runtimeProfileDir != null) {
                    File runtimeProfileFile = new File(runtimeProfileDir, profileFileName);

                    if (!runtimeProfileFile.getCanonicalPath().equals(profileFile.getCanonicalPath())) {
                        Files.copy(profileFile.toPath(), runtimeProfileFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            List<String> licenseFilesArray = new ArrayList<>();
            Collection<Part> parts = request.getParts();
            int index = 0;

            for (Part part : parts) {
                if ("docFiles[]".equals(part.getName()) && part.getSize() > 0 && part.getSubmittedFileName() != null && !part.getSubmittedFileName().isEmpty()) {
                    String submittedDocName = sanitizeFileName(part.getSubmittedFileName());
                    String ext = getFileExtension(submittedDocName);
                    String docFileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + "_" + index + ext;

                    File docFile = new File(licenceDir, docFileName);

                    try (InputStream inputStream = part.getInputStream()) {
                        Files.copy(inputStream, docFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    if (runtimeLicenceDir != null) {
                        File runtimeDocFile = new File(runtimeLicenceDir, docFileName);

                        if (!runtimeDocFile.getCanonicalPath().equals(docFile.getCanonicalPath())) {
                            Files.copy(docFile.toPath(), runtimeDocFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }

                    licenseFilesArray.add(docFileName);
                    index++;
                }
            }

            String licenseFilesStr = String.join(",", licenseFilesArray);

            Vendor vendor = new Vendor();
            vendor.setName(name);
            vendor.setEmail(email);
            vendor.setNophone(phoneFormatted);
            vendor.setPassword(password);
            vendor.setDocPath(licenseFilesStr);
            vendor.setProfilePath(profileFileName);
            vendor.setAddress(address);
            vendor.setPostcode(postcode);
            vendor.setCity(city);
            vendor.setState(state);
            vendor.setLatitude(latitude);
            vendor.setLongitude(longitude);

            if (vendorDAO.registerVendor(vendor)) {
                sendJson(response, "success", "Registration successful!");
            } else {
                sendJson(response, "error", "Registration failed at database level.");
            }

        } catch (SQLException e) {
            sendJson(response, "error", "Database Error: " + e.getMessage());
        } catch (Exception e) {
            sendJson(response, "error", "Server Error: " + e.getMessage());
        }
    }

    private File getRuntimeDir(String folderName) {
        String runtimePath = getServletContext().getRealPath("/" + folderName);

        if (runtimePath == null || runtimePath.trim().isEmpty()) {
            return null;
        }

        File runtimeDir = new File(runtimePath);

        if (!runtimeDir.exists()) {
            runtimeDir.mkdirs();
        }

        return runtimeDir;
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

    private void sendJson(HttpServletResponse response, String status, String message) throws IOException {
        PrintWriter out = response.getWriter();
        out.print("{\"status\":\"" + jsonEscape(status) + "\",\"message\":\"" + jsonEscape(message) + "\"}");
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