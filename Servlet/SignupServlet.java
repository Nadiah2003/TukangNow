package Servlet;

import DAO.CustomerDAO;
import Model.Customer;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/api/signup")
public class SignupServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private CustomerDAO customerDAO;
    private Gson gson;

    public void init() {
        customerDAO = new CustomerDAO();
        gson = new Gson();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        Map<String, String> jsonResponse = new HashMap<>();

        String fullname = request.getParameter("fullname");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String phoneInput = request.getParameter("phone");

        if (email == null || phoneInput == null || fullname == null || password == null ||
                email.trim().isEmpty() || phoneInput.trim().isEmpty() || fullname.trim().isEmpty() || password.trim().isEmpty()) {
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "Sila lengkapkan semua butiran maklumat borang pendaftaran.");
            out.print(gson.toJson(jsonResponse));
            return;
        }

        String phoneOnly = phoneInput.replaceAll("[^0-9]", "");
        String phoneFormatted = "";

        if (!phoneOnly.startsWith("60")) {
            if (phoneOnly.startsWith("0")) {
                phoneFormatted = "60" + phoneOnly.substring(1);
            } else {
                phoneFormatted = "60" + phoneOnly;
            }
        } else {
            phoneFormatted = phoneOnly;
        }

        try {
            if (customerDAO.isDuplicate(phoneFormatted, email)) {
                jsonResponse.put("status", "error");
                jsonResponse.put("message", "Nombor telefon atau E-mel telah pun didaftarkan!");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            String address = request.getParameter("address");
            String postcode = request.getParameter("postcode");
            String city = request.getParameter("city");
            String state = request.getParameter("state");

            double latitude = parseDouble(request.getParameter("latitude"));
            double longitude = parseDouble(request.getParameter("longitude"));

            if (latitude == 0.0 || longitude == 0.0) {
                jsonResponse.put("status", "error");
                jsonResponse.put("message", "Latitude dan longitude tidak sah. Sila semak semula alamat anda.");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            Customer newCustomer = new Customer(fullname, email, phoneFormatted, password);
            newCustomer.setAddress(address);
            newCustomer.setPostcode(postcode);
            newCustomer.setCity(city);
            newCustomer.setState(state);
            newCustomer.setLatitude(latitude);
            newCustomer.setLongitude(longitude);

            if (customerDAO.registerCustomer(newCustomer)) {
                jsonResponse.put("status", "success");
                jsonResponse.put("message", "Registration successful!");
            } else {
                jsonResponse.put("status", "error");
                jsonResponse.put("message", "Gagal menyelesaikan proses pendaftaran.");
            }

        } catch (Exception e) {
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "Ralat sistem pangkalan data: " + e.getMessage());
        }

        out.print(gson.toJson(jsonResponse));
        out.flush();
    }

    private double parseDouble(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return 0.0;
            }

            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }
}