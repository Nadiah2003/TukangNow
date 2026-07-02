package DAO;

import Config.ConnectionManager;
import Model.ServiceModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ServiceDAO {

    public ServiceModel getServiceByVendorId(int vendorId) throws Exception {
        ServiceModel service = null;
        String sql = "SELECT servicename, subservice FROM service WHERE vendor_id = ?";
        
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, vendorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    service = new ServiceModel();
                    service.setServiceName(rs.getString("servicename"));
                    
                    String existing = rs.getString("subservice");
                    if (existing == null || existing.trim().equalsIgnoreCase("null")) {
                        existing = "";
                    }
                    service.setSubService(existing);
                }
            }
        }
        return service;
    }

    public int updateSubServices(int vendorId, String subServices) throws Exception {
        String sql = "UPDATE service SET subservice = ? WHERE vendor_id = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, subServices);
            stmt.setInt(2, vendorId);
            return stmt.executeUpdate();
        }
    }
}