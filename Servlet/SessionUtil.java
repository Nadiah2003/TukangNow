package Servlet;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SessionUtil {
    private static final Gson gson = new Gson();

    public static boolean isSessionExpired(HttpSession session, HttpServletResponse response, String sessionKey, String redirectPage) throws IOException {
        if (session == null || session.getAttribute(sessionKey) == null) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("status", "session_expired");
            map.put("message", "Session expired. Please login again.");
            map.put("redirect", redirectPage);

            PrintWriter out = response.getWriter();
            out.print(gson.toJson(map));
            out.flush();

            return true;
        }

        return false;
    }
}