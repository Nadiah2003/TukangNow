package Model;

public class ServiceModel {
    private int id;
    private int vendorId;
    private String serviceName;
    private String subService;

    public ServiceModel() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVendorId() { return vendorId; }
    public void setVendorId(int vendorId) { this.vendorId = vendorId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getSubService() { return subService; }
    public void setSubService(String subService) { this.subService = subService; }
}