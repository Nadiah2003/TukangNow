package Model;

public class AddServiceInfo {
    private String status;
    private String message;
    private String mainCategory;
    private String existingSubServices;

    public AddServiceInfo() {}

    public AddServiceInfo(String status, String message, String mainCategory, String existingSubServices) {
        this.status = status;
        this.message = message;
        this.mainCategory = mainCategory;
        this.existingSubServices = existingSubServices;
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

    public String getMainCategory() {
        return mainCategory;
    }

    public void setMainCategory(String mainCategory) {
        this.mainCategory = mainCategory;
    }    

    public String getExistingSubServices() {
        return existingSubServices;
    }

    public void setExistingSubServices(String existingSubServices) {
        this.existingSubServices = existingSubServices;
    }
}