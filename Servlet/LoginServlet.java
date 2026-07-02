package Servlet;

import DAO.LoginDAO;
import Model.UserSession;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "LoginServlet", urlPatterns = {"/api/login"})
public class LoginServlet extends HttpServlet {

    private final LoginDAO loginDAO = new LoginDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        setJsonResponse(request, response);

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        PrintWriter out = response.getWriter();

        if (email == null || password == null || email.trim().isEmpty() || password.isEmpty()) {
            out.print(gson.toJson(new ResponseMessage("error", "Email and Password are required")));
            out.flush();
            return;
        }

        UserSession userSession = loginDAO.authenticateUser(email.trim(), password);

        if ("success".equalsIgnoreCase(userSession.getStatus())) {
            HttpSession oldSession = request.getSession(false);

            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(12 * 60 * 60);

            String role = safe(userSession.getRole());
            String cleanRole = role.toLowerCase().trim();
            int userId = userSession.getUserId();
            String userName = safe(userSession.getName());
            String state = safe(userSession.getState());
            String adminLevel = String.valueOf(userSession.getAdminLevel());

            session.setAttribute("userId", userId);
            session.setAttribute("id", userId);
            session.setAttribute("userName", userName);
            session.setAttribute("name", userName);
            session.setAttribute("role", role);
            session.setAttribute("userRole", role);
            session.setAttribute("accountType", role);
            session.setAttribute("userType", role);
            session.setAttribute("loginType", role);
            session.setAttribute("state", state);
            session.setAttribute("adminLevel", userSession.getAdminLevel());

            if (cleanRole.contains("admin")) {
                session.setAttribute("adminId", userId);
                session.setAttribute("admin_id", userId);
                session.setAttribute("adminID", userId);
            }

            if (cleanRole.contains("customer")) {
                session.setAttribute("customerId", userId);
                session.setAttribute("customer_id", userId);
                session.setAttribute("customerID", userId);
            }

            if (cleanRole.contains("vendor")) {
                session.setAttribute("vendorId", userId);
                session.setAttribute("vendor_id", userId);
                session.setAttribute("vendorID", userId);
            }

            addLoginCookie(request, response, "tn_userId", String.valueOf(userId));
            addLoginCookie(request, response, "tn_id", String.valueOf(userId));
            addLoginCookie(request, response, "tn_userName", userName);
            addLoginCookie(request, response, "tn_name", userName);
            addLoginCookie(request, response, "tn_role", role);
            addLoginCookie(request, response, "tn_userRole", role);
            addLoginCookie(request, response, "tn_accountType", role);
            addLoginCookie(request, response, "tn_userType", role);
            addLoginCookie(request, response, "tn_loginType", role);
            addLoginCookie(request, response, "tn_state", state);
            addLoginCookie(request, response, "tn_adminLevel", adminLevel);

            if (cleanRole.contains("admin")) {
                addLoginCookie(request, response, "tn_adminId", String.valueOf(userId));
                addLoginCookie(request, response, "tn_admin_id", String.valueOf(userId));
                addLoginCookie(request, response, "tn_adminID", String.valueOf(userId));
            }

            if (cleanRole.contains("customer")) {
                addLoginCookie(request, response, "tn_customerId", String.valueOf(userId));
                addLoginCookie(request, response, "tn_customer_id", String.valueOf(userId));
                addLoginCookie(request, response, "tn_customerID", String.valueOf(userId));
            }

            if (cleanRole.contains("vendor")) {
                addLoginCookie(request, response, "tn_vendorId", String.valueOf(userId));
                addLoginCookie(request, response, "tn_vendor_id", String.valueOf(userId));
                addLoginCookie(request, response, "tn_vendorID", String.valueOf(userId));
            }
        }

        out.print(gson.toJson(userSession));
        out.flush();
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        setJsonResponse(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void setJsonResponse(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");

        response.setHeader("Access-Control-Allow-Origin", origin == null ? "*" : origin);
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setContentType("application/json; charset=UTF-8");
    }

    private void addLoginCookie(HttpServletRequest request, HttpServletResponse response, String name, String value) {
        String cleanValue = encodeCookieValue(safe(value));
        String contextPath = request.getContextPath();

        if (contextPath == null || contextPath.trim().isEmpty()) {
            contextPath = "/";
        }

        Cookie contextCookie = new Cookie(name, cleanValue);
        contextCookie.setPath(contextPath);
        contextCookie.setMaxAge(12 * 60 * 60);
        contextCookie.setHttpOnly(false);
        response.addCookie(contextCookie);

        Cookie rootCookie = new Cookie(name, cleanValue);
        rootCookie.setPath("/");
        rootCookie.setMaxAge(12 * 60 * 60);
        rootCookie.setHttpOnly(false);
        response.addCookie(rootCookie);
    }

    private String encodeCookieValue(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private static class ResponseMessage {
        String status;
        String message;

        ResponseMessage(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }
}