package Model;

public class Order {
    private int id;
    private String serviceName;
    private String problem;
    private String date;
    private String bookingdate;
    private String bookingDate;
    private String bookingTime;
    private String status;
    private double deposit;

    public Order() {}

    public Order(int id, String serviceName, String problem, String date, String status, double deposit) {
        this.id = id;
        this.serviceName = serviceName;
        this.problem = problem;
        this.date = date;
        this.bookingdate = date;
        this.status = status;
        this.deposit = deposit;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }   

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }   

    public String getBookingdate() {
        return bookingdate;
    }

    public void setBookingdate(String bookingdate) {
        this.bookingdate = bookingdate;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(String bookingTime) {
        this.bookingTime = bookingTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getDeposit() {
        return deposit;
    }

    public void setDeposit(double deposit) {
        this.deposit = deposit;
    }
}