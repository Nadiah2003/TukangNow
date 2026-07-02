package Servlet;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/LogoutServlet")
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);

        String role = "";
        String redirectUrl = request.getContextPath() + "/login.html";

        if (session != null && session.getAttribute("role") != null) {
            role = session.getAttribute("role").toString().trim().toLowerCase();
        }

        if ("customer".equals(role)) {
            redirectUrl = request.getContextPath() + "/index.html";
        }

        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect(redirectUrl);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        String role = "";
        String redirectUrl = "login.html";

        if (session != null && session.getAttribute("role") != null) {
            role = session.getAttribute("role").toString().trim().toLowerCase();
        }

        if ("customer".equals(role)) {
            redirectUrl = "index.html";
        }

        if (session != null) {
            session.invalidate();
        }

        Map<String, String> map = new LinkedHashMap<>();
        map.put("status", "success");
        map.put("message", "Logged out successfully.");
        map.put("redirectUrl", redirectUrl);

        PrintWriter out = response.getWriter();
        out.print(gson.toJson(map));
        out.flush();
    }
}