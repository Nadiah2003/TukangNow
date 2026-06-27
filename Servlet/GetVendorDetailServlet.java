package Servlet;

import DAO.GetVendorDetailDAO;
import Model.VendorServiceDetail;
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
import Servlet.SessionUtil;

@WebServlet("/GetVendorDetailServlet")
public class GetVendorDetailServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private GetVendorDetailDAO getVendorDetailDAO;
    private final Gson gson = new Gson();

    @Override
    public void init() {
        getVendorDetailDAO = new GetVendorDetailDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "index.html")) {
            return;
        }

        String idParam = request.getParameter("id");

        try {
            if (idParam == null || idParam.trim().isEmpty()) {
                sendJson(response, errorResponse("Missing vendor id."));
                return;
            }

            int vendorId = Integer.parseInt(idParam);
            VendorServiceDetail detail = getVendorDetailDAO.getVendorDetail(vendorId);

            if (detail == null) {
                sendJson(response, errorResponse("Vendor not found."));
                return;
            }

            detail.setStatus("success");
            sendJson(response, gson.toJson(detail));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sendJson(response, errorResponse(e.getMessage()));
        }
    }

    private String errorResponse(String message) {
        Map<String, String> map = new LinkedHashMap<>();
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