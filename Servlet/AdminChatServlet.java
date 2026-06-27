package Servlet;

import DAO.AdminChatDAO;
import Model.AdminChat;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "AdminChatServlet", urlPatterns = {"/AdminChatServlet"})
public class AdminChatServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private AdminChatDAO adminChatDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        adminChatDAO = new AdminChatDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            sendJson(response, sessionExpiredResponse(request));
            return;
        }

        try {
            int customerId = Integer.parseInt(session.getAttribute("userId").toString());
            String bookingIdText = request.getParameter("booking_id");

            if (bookingIdText == null || bookingIdText.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendJson(response, errorResponse("Invalid booking ID."));
                return;
            }

            int bookingId = Integer.parseInt(bookingIdText);

            if (bookingId <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendJson(response, errorResponse("Invalid booking ID."));
                return;
            }

            ArrayList<AdminChat> chatList = adminChatDAO.getChats(customerId, bookingId);
            sendJson(response, gson.toJson(chatList));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, errorResponse("Invalid booking ID."));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse(e.getMessage() == null ? "Unable to load chat." : e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            sendJson(response, sessionExpiredResponse(request));
            return;
        }

        try {
            int customerId = Integer.parseInt(session.getAttribute("userId").toString());
            JsonObject jsonObject = gson.fromJson(request.getReader(), JsonObject.class);

            if (jsonObject == null || !jsonObject.has("booking_id") || !jsonObject.has("message")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendJson(response, errorResponse("Invalid request."));
                return;
            }

            int bookingId = jsonObject.get("booking_id").getAsInt();
            String message = jsonObject.get("message").getAsString();

            if (bookingId <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendJson(response, errorResponse("Invalid booking ID."));
                return;
            }

            if (message == null || message.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendJson(response, errorResponse("Message cannot be empty."));
                return;
            }

            adminChatDAO.sendCustomerMessage(customerId, bookingId, message);

            sendJson(response, successResponse("Message sent successfully."));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, errorResponse("Invalid booking ID."));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse(e.getMessage() == null ? "Unable to send message." : e.getMessage()));
        }
    }

    private String sessionExpiredResponse(HttpServletRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "session_expired");
        map.put("redirect", request.getContextPath() + "/index.html");
        return gson.toJson(map);
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
        map.put("message", message);
        return gson.toJson(map);
    }

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private void sendJson(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
}