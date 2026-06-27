package Model;

public class ProfileVendorData {
    private String status;
    private String message;
    private ProfileVendorInfo vendor;
    private ProfileServiceInfo service;
    private int jobsCompleted;
    private String avgRating;
    private int totalReviews;

    public ProfileVendorData() {}

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

    public ProfileVendorInfo getVendor() {
        return vendor;
    }

    public void setVendor(ProfileVendorInfo vendor) {
        this.vendor = vendor;
    }

    public ProfileServiceInfo getService() {
        return service;
    }

    public void setService(ProfileServiceInfo service) {
        this.service = service;
    }

    public int getJobsCompleted() {
        return jobsCompleted;
    }

    public void setJobsCompleted(int jobsCompleted) {
        this.jobsCompleted = jobsCompleted;
    }

    public String getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(String avgRating) {
        this.avgRating = avgRating;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }
}