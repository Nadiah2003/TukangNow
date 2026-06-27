package Servlet;

import DAO.JobHistoryDAO;
import Model.JobHistory;
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

@WebServlet(name = "JobHistoryServlet", urlPatterns = {"/JobHistoryServlet"})
public class JobHistoryServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private JobHistoryDAO jobHistoryDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        jobHistoryDAO = new JobHistoryDAO();
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
            int vendorId = Integer.parseInt(session.getAttribute("userId").toString());
            ArrayList<JobHistory> jobHistoryList = jobHistoryDAO.getCompletedJobHistory(vendorId);
            sendJson(response, gson.toJson(jobHistoryList));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse("Unable to load job history."));
        }
    }

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
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
        map.put("message", message);
        return gson.toJson(map);
    }

    private void sendJson(HttpServletResponse response, String json) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
}