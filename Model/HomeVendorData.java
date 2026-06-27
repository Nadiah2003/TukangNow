package Model;

import java.util.ArrayList;

public class HomeVendorData {
    private String status;
    private String message;
    private HomeVendorInfo vendor;
    private ArrayList<VendorJob> urgentJobs;
    private ArrayList<VendorJob> pendingJobs;
    private ArrayList<VendorJob> activeJobs;
    private int unreadNotificationsCount;

    public HomeVendorData() {
        this.urgentJobs = new ArrayList<>();
        this.pendingJobs = new ArrayList<>();
        this.activeJobs = new ArrayList<>();
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

    public HomeVendorInfo getVendor() {
        return vendor;
    }

    public void setVendor(HomeVendorInfo vendor) {
        this.vendor = vendor;
    }

    public ArrayList<VendorJob> getUrgentJobs() {
        return urgentJobs;
    }

    public void setUrgentJobs(ArrayList<VendorJob> urgentJobs) {
        this.urgentJobs = urgentJobs;
    }

    public ArrayList<VendorJob> getPendingJobs() {
        return pendingJobs;
    }

    public void setPendingJobs(ArrayList<VendorJob> pendingJobs) {
        this.pendingJobs = pendingJobs;
    }

    public ArrayList<VendorJob> getActiveJobs() {
        return activeJobs;
    }

    public void setActiveJobs(ArrayList<VendorJob> activeJobs) {
        this.activeJobs = activeJobs;
    }

    public int getUnreadNotificationsCount() {
        return unreadNotificationsCount;
    }

    public void setUnreadNotificationsCount(int unreadNotificationsCount) {
        this.unreadNotificationsCount = unreadNotificationsCount;
    }
}