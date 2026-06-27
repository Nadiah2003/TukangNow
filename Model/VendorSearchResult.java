package Model;

public class VendorSearchResult {
    private String vendorName;
    private double distance;
    private String subservice;
    private String state;
    private String availTime;

    public VendorSearchResult(String vendorName, double distance, String subservice, String state, String availTime) {
        this.vendorName = vendorName;
        this.distance = distance;
        this.subservice = subservice;
        this.state = state;
        this.availTime = availTime;
    }

    // Getter dan Setter untuk keperluan penukaran kepada JSON (Gson)
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
    public String getSubservice() { return subservice; }
    public void setSubservice(String subservice) { this.subservice = subservice; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getAvailTime() { return availTime; }
    public void setAvailTime(String availTime) { this.availTime = availTime; }
}