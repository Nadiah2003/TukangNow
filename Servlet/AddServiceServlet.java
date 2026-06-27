package Servlet;

import DAO.ServiceDAO;
import Model.ServiceModel;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import Servlet.SessionUtil;

@WebServlet("/AddServiceServlet")
public class AddServiceServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final ServiceDAO serviceDAO = new ServiceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "login.html")) {
            return;
        }

        try (PrintWriter out = response.getWriter()) {
            int vendorId = Integer.parseInt(session.getAttribute("userId").toString());
            ServiceModel service = serviceDAO.getServiceByVendorId(vendorId);

            if (service != null) {
                out.print(String.format(
                        "{\"status\":\"success\",\"mainCategory\":\"%s\",\"existingSubServices\":\"%s\"}",
                        service.getServiceName(),
                        service.getSubService()
                ));
            } else {
                out.print("{\"status\":\"error\",\"message\":\"No service record found for this vendor.\"}");
            }
        } catch (Exception e) {
            response.getWriter().print("{\"status\":\"error\",\"message\":\"Database error: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (SessionUtil.isSessionExpired(session, response, "userId", "login.html")) {
            return;
        }

        try (PrintWriter out = response.getWriter()) {
            int vendorId = Integer.parseInt(session.getAttribute("userId").toString());
            String subservices = request.getParameter("subservices");

            if (subservices == null) {
                subservices = "";
            }

            int affectedRows = serviceDAO.updateSubServices(vendorId, subservices);

            if (affectedRows > 0) {
                out.print("{\"status\":\"success\",\"message\":\"Services updated successfully.\"}");
            } else {
                out.print("{\"status\":\"error\",\"message\":\"Update failed. No record found or no changes made.\"}");
            }
        } catch (Exception e) {
            response.getWriter().print("{\"status\":\"error\",\"message\":\"Server error during update: " + e.getMessage() + "\"}");
        }
    }
}