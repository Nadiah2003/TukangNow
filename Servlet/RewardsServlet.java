package Servlet;

import DAO.RewardsDAO;
import Model.RedeemRewardResult;
import Model.RewardsPageData;
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

@WebServlet("/RewardsServlet")
public class RewardsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private RewardsDAO rewardsDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        rewardsDAO = new RewardsDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setJsonResponse(response);

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "index.html")) {
            return;
        }

        try {
            int customerId = Integer.parseInt(session.getAttribute("userId").toString());
            RewardsPageData data = rewardsDAO.getRewardsPageData(customerId);
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

        try {
            int customerId = Integer.parseInt(session.getAttribute("userId").toString());
            String action = safe(request.getParameter("action"));

            if (!"redeem".equalsIgnoreCase(action)) {
                sendJson(response, errorResponse("Invalid action."));
                return;
            }

            int rewardId = parseInt(request.getParameter("reward_id"));

            if (rewardId <= 0) {
                sendJson(response, errorResponse("Missing voucher information."));
                return;
            }

            RedeemRewardResult result = rewardsDAO.redeemReward(customerId, rewardId);
            sendJson(response, gson.toJson(result));

        } catch (Exception e) {
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
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

    private String errorResponse(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "error");
        map.put("message", message == null ? "Unknown error." : message);
        return gson.toJson(map);
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