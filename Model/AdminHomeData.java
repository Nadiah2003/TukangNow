package Model;

import java.util.ArrayList;
import java.util.List;

public class AdminHomeData {
    private String adminProfile;
    private int newApps;
    private int activeVendors;
    private int newReports;
    private List<AdminHomeVendor> vendors;

    public AdminHomeData() {
        this.vendors = new ArrayList<>();
    }

    public String getAdminProfile() {
        return adminProfile;
    }

    public void setAdminProfile(String adminProfile) {
        this.adminProfile = adminProfile;
    }

    public int getNewApps() {
        return newApps;
    }

    public void setNewApps(int newApps) {
        this.newApps = newApps;
    }

    public int getActiveVendors() {
        return activeVendors;
    }

    public void setActiveVendors(int activeVendors) {
        this.activeVendors = activeVendors;
    }

    public int getNewReports() {
        return newReports;
    }

    public void setNewReports(int newReports) {
        this.newReports = newReports;
    }

    public List<AdminHomeVendor> getVendors() {
        return vendors;
    }

    public void setVendors(List<AdminHomeVendor> vendors) {
        this.vendors = vendors;
    }
}