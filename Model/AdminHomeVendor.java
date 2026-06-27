package Model;

public class AdminHomeVendor {
    private String name;
    private String phone;
    private String docUrl;
    private String profilePath;
    private String status;
    private String rating;
    private String workDone;

    public AdminHomeVendor() {}

    public AdminHomeVendor(String name, String phone, String docUrl, String profilePath, String status, String rating, String workDone) {
        this.name = name;
        this.phone = phone;
        this.docUrl = docUrl;
        this.profilePath = profilePath;
        this.status = status;
        this.rating = rating;
        this.workDone = workDone;
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

    public String getDocUrl() {
        return docUrl;
    }

    public void setDocUrl(String docUrl) {
        this.docUrl = docUrl;
    }

    public String getProfilePath() {
        return profilePath;
    }

    public void setProfilePath(String profilePath) {
        this.profilePath = profilePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getWorkDone() {
        return workDone;
    }

    public void setWorkDone(String workDone) {
        this.workDone = workDone;
    }
}