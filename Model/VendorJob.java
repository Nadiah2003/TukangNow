package Model;

public class VendorJob {
    private int bookingId;
    private String title;
    private String subservice;
    private String problem;
    private double deposit;
    private String fullSchedule;
    private String custName;
    private String timeAgo;
    private String distanceKM;
    private String status;

    public VendorJob() {}

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubservice() {
        return subservice;
    }

    public void setSubservice(String subservice) {
        this.subservice = subservice;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }

    public double getDeposit() {
        return deposit;
    }

    public void setDeposit(double deposit) {
        this.deposit = deposit;
    }

    public String getFullSchedule() {
        return fullSchedule;
    }

    public void setFullSchedule(String fullSchedule) {
        this.fullSchedule = fullSchedule;
    }

    public String getCustName() {
        return custName;
    }

    public void setCustName(String custName) {
        this.custName = custName;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public String getDistanceKM() {
        return distanceKM;
    }

    public void setDistanceKM(String distanceKM) {
        this.distanceKM = distanceKM;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}