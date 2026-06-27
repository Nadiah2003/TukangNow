package Model;

import java.util.LinkedHashMap;
import java.util.Map;

public class EmergencyCustomerData {
    private String status;
    private String message;
    private int id;
    private String custName;
    private String custEmail;
    private String custPhone;
    private String custAddress;
    private Map<String, String> serviceIds;

    public EmergencyCustomerData() {
        serviceIds = new LinkedHashMap<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getId() {
        return id;
    }   

    public void setId(int id) {
        this.id = id;
    }

    public String getCustName() {
        return custName;
    }

    public void setCustName(String custName) {
        this.custName = custName;
    }

    public String getCustEmail() {
        return custEmail;
    }

    public void setCustEmail(String custEmail) {
        this.custEmail = custEmail;
    }

    public String getCustPhone() {
        return custPhone;
    }

    public void setCustPhone(String custPhone) {
        this.custPhone = custPhone;
    }

    public String getCustAddress() {
        return custAddress;
    }

    public void setCustAddress(String custAddress) {
        this.custAddress = custAddress;
    }

    public Map<String, String> getServiceIds() {
        return serviceIds;
    }

    public void setServiceIds(Map<String, String> serviceIds) {
        this.serviceIds = serviceIds;
    }

    public void addServiceIds(String category, String serviceIds) {
        this.serviceIds.put(category, serviceIds);
    }
}