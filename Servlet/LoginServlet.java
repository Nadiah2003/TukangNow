package Servlet;

import DAO.LoginDAO;
import Model.UserSession;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
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

        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin") == null ? "*" : request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setContentType("application/json; charset=UTF-8");

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        PrintWriter out = response.getWriter();

        if (email == null || password == null || email.trim().isEmpty() || password.isEmpty()) {
            out.print(gson.toJson(new ResponseMessage("error", "Email and Password are required")));
            out.flush();
            return;
        }

        UserSession userSession = loginDAO.authenticateUser(email.trim(), password);

        if ("success".equals(userSession.getStatus())) {
            HttpSession session = request.getSession(true);
            session.setAttribute("userId", userSession.getUserId());
            session.setAttribute("userName", userSession.getName());
            session.setAttribute("role", userSession.getRole());
            session.setAttribute("state", userSession.getState());

            if ("admin".equalsIgnoreCase(userSession.getRole())) {
                session.setAttribute("adminLevel", userSession.getAdminLevel());
            }
        }

        out.print(gson.toJson(userSession));
        out.flush();
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin") == null ? "*" : request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setStatus(HttpServletResponse.SC_OK);
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