package Servlet;

import DAO.AdminProfileChatDAO;
import Model.AdminProfileChatConversation;
import Model.AdminProfileChatMessage;
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

@WebServlet(name = "AdminProfileChatServlet", urlPatterns = {"/AdminProfileChatServlet"})
public class AdminProfileChatServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private AdminProfileChatDAO adminProfileChatDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        adminProfileChatDAO = new AdminProfileChatDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            sendJson(response, sessionExpiredResponse(request));
            return;
        }

        String action = request.getParameter("action");

        if (action == null || action.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, errorResponse("Invalid action."));
            return;
        }

        try {
            int adminId = Integer.parseInt(session.getAttribute("userId").toString());

            if ("badge".equals(action)) {
                int unreadCount = adminProfileChatDAO.getUnreadCountForAdmin(adminId);
                sendJson(response, badgeResponse(unreadCount));
                return;
            }

            if ("conversations".equals(action)) {
                ArrayList<AdminProfileChatConversation> conversations = adminProfileChatDAO.getConversations(adminId);
                sendJson(response, conversationsResponse(conversations));
                return;
            }

            if ("messages".equals(action)) {
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

                ArrayList<AdminProfileChatMessage> messages = adminProfileChatDAO.getMessages(adminId, bookingId);
                sendJson(response, messagesResponse(messages));
                return;
            }

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, errorResponse("Invalid action."));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, errorResponse("Invalid booking ID."));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse(e.getMessage() == null ? "Unable to process chat." : e.getMessage()));
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
            int adminId = Integer.parseInt(session.getAttribute("userId").toString());
            JsonObject jsonObject = gson.fromJson(request.getReader(), JsonObject.class);

            if (jsonObject == null || !jsonObject.has("action")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendJson(response, errorResponse("Invalid request."));
                return;
            }

            String action = jsonObject.get("action").getAsString();

            if (!"sendMessage".equals(action)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendJson(response, errorResponse("Invalid action."));
                return;
            }

            if (!jsonObject.has("booking_id") || !jsonObject.has("message")) {
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

            adminProfileChatDAO.sendAdminMessage(adminId, bookingId, message);
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

    private String badgeResponse(int unreadCount) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "success");
        map.put("unreadCount", unreadCount);
        map.put("hasUnread", unreadCount > 0);
        return gson.toJson(map);
    }

    private String conversationsResponse(ArrayList<AdminProfileChatConversation> conversations) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "success");
        map.put("conversations", conversations);
        return gson.toJson(map);
    }

    private String messagesResponse(ArrayList<AdminProfileChatMessage> messages) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "success");
        map.put("messages", messages);
        return gson.toJson(map);
    }

    private String successResponse(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "success");
        map.put("message", message);
        return gson.toJson(map);
    }

    private String sessionExpiredResponse(HttpServletRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "session_expired");
        map.put("redirect", request.getContextPath() + "/login.html");
        return gson.toJson(map);
    }

    private String errorResponse(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "error");
        map.put("message", message == null ? "Something went wrong." : message);
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