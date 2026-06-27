package Model;

public class DetailVendor {
    private String status;
    private String message;
    private int id;
    private String name;
    private String phone;
    private String email;
    private String profilePath;
    private String licenseUrl;

    public DetailVendor() {}

    public DetailVendor(String status, String message, int id, String name, String phone, String email, String profilePath, String licenseUrl) {
        this.status = status;
        this.message = message;
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.profilePath = profilePath;
        this.licenseUrl = licenseUrl;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }    

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }    

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }    

    public String getProfilePath() {
        return profilePath;
    }

    public void setProfilePath(String profilePath) {
        this.profilePath = profilePath;
    }    

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }
}