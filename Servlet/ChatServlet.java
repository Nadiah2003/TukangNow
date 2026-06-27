package Servlet;

import DAO.ChatDAO;
import Model.ChatInfo;
import Model.ChatMessage;
import com.google.gson.Gson;
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

@WebServlet("/ChatServlet")
public class ChatServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private ChatDAO chatDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        chatDAO = new ChatDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        String requestedViewForRedirect = safe(request.getParameter("view"));
        String redirectPage = "vendor".equalsIgnoreCase(requestedViewForRedirect) ? "login.html" : "index.html";

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", redirectPage)) {
            return;
        }

        try {
            int sessionUserId = Integer.parseInt(session.getAttribute("userId").toString());
            String sessionRole = session.getAttribute("role") == null ? "" : session.getAttribute("role").toString();
            String action = safeDefault(request.getParameter("action"), "fetch");
            int bookingId = parseInt(request.getParameter("booking_id"));
            String requestedView = safe(request.getParameter("view"));

            if (bookingId <= 0) {
                sendJson(response, errorResponse("Booking ID is required."));
                return;
            }

            ChatInfo chatInfo = chatDAO.getChatInfo(sessionUserId, sessionRole, requestedView, bookingId);

            if (chatInfo == null) {
                sendJson(response, errorResponse("You are not allowed to access this chat."));
                return;
            }

            if ("init".equalsIgnoreCase(action)) {
                sendJson(response, gson.toJson(chatInfo));
                return;
            }

            if ("fetch".equalsIgnoreCase(action)) {
                ArrayList<ChatMessage> messages = chatDAO.getMessages(sessionUserId, chatInfo.getViewer_role(), bookingId);

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "success");
                result.put("booking_id", bookingId);
                result.put("viewer_role", chatInfo.getViewer_role());
                result.put("partner_name", chatInfo.getPartner_name());
                result.put("messages", messages);

                sendJson(response, gson.toJson(result));
                return;
            }

            sendJson(response, errorResponse("Invalid action."));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        setJsonResponse(response);

        String requestedViewForRedirect = safe(request.getParameter("view"));
        String redirectPage = "vendor".equalsIgnoreCase(requestedViewForRedirect) ? "login.html" : "index.html";

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", redirectPage)) {
            return;
        }

        try {
            int sessionUserId = Integer.parseInt(session.getAttribute("userId").toString());
            String sessionRole = session.getAttribute("role") == null ? "" : session.getAttribute("role").toString();
            String action = safe(request.getParameter("action"));
            int bookingId = parseInt(request.getParameter("booking_id"));
            int msgId = parseInt(request.getParameter("msg_id"));
            String requestedView = safe(request.getParameter("view"));
            String message = safe(request.getParameter("message"));

            if (bookingId <= 0) {
                sendJson(response, errorResponse("Booking ID is required."));
                return;
            }

            ChatInfo chatInfo = chatDAO.getChatInfo(sessionUserId, sessionRole, requestedView, bookingId);

            if (chatInfo == null) {
                sendJson(response, errorResponse("You are not allowed to access this chat."));
                return;
            }

            if ("send".equalsIgnoreCase(action)) {
                if (message.isEmpty()) {
                    sendJson(response, errorResponse("Message cannot be empty."));
                    return;
                }

                boolean sent = chatDAO.sendMessage(bookingId, sessionUserId, chatInfo.getViewer_role(), message);

                if (sent) {
                    sendJson(response, successResponse("Message sent."));
                } else {
                    sendJson(response, errorResponse("Failed to send message."));
                }

                return;
            }

            if ("update_single".equalsIgnoreCase(action)) {
                if (msgId <= 0) {
                    sendJson(response, errorResponse("Message ID is required."));
                    return;
                }

                if (message.isEmpty()) {
                    sendJson(response, errorResponse("Message cannot be empty."));
                    return;
                }

                boolean updated = chatDAO.updateMessage(bookingId, msgId, sessionUserId, chatInfo.getViewer_role(), message);

                if (updated) {
                    sendJson(response, successResponse("Message updated."));
                } else {
                    sendJson(response, errorResponse("You can only edit your own message."));
                }

                return;
            }

            if ("delete_single".equalsIgnoreCase(action)) {
                if (msgId <= 0) {
                    sendJson(response, errorResponse("Message ID is required."));
                    return;
                }

                boolean deleted = chatDAO.deleteMessage(bookingId, msgId, sessionUserId, chatInfo.getViewer_role());

                if (deleted) {
                    sendJson(response, successResponse("Message deleted."));
                } else {
                    sendJson(response, errorResponse("You can only delete your own message."));
                }

                return;
            }

            sendJson(response, errorResponse("Invalid action."));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
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

    private int parseInt(String value) {
        try {
            if (value != null && !value.trim().isEmpty()) {
                return Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException e) {
            return 0;
        }

        return 0;
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