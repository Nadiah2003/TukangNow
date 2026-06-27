package Model;

import java.util.List;

public class ProfileServiceInfo {
    private String servicename;
    private String subServices;
    private double startprice;
    private String avail_time;
    private String avail_date;
    private List<String> subServicesList;

    public ProfileServiceInfo() {}

    public String getServicename() {
        return servicename;
    }

    public void setServicename(String servicename) {
        this.servicename = servicename;
    }

    public String getSubServices() {
        return subServices;
    }

    public void setSubServices(String subServices) {
        this.subServices = subServices;
    }

    public double getStartprice() {
        return startprice;
    }

    public void setStartprice(double startprice) {
        this.startprice = startprice;
    }

    public String getAvail_time() {
        return avail_time;
    }

    public void setAvail_time(String avail_time) {
        this.avail_time = avail_time;
    }

    public String getAvail_date() {
        return avail_date;
    }

    public void setAvail_date(String avail_date) {
        this.avail_date = avail_date;
    }

    public List<String> getSubServicesList() {
        return subServicesList;
    }

    public void setSubServicesList(List<String> subServicesList) {
        this.subServicesList = subServicesList;
    }
}