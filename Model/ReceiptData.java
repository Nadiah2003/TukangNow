package Model;

public class ReceiptData {
    private int id;
    private String bookingdate;
    private String subservicebooked;
    private String vendor_name;
    private String problem;
    private String status;
    private double deposit;
    private double travelfee;
    private double totalamount;
    private double paymentpaid;
    private String paymentmethod;
    private String paymentdate;
    private String payment_reference;
    private double distancekm;

    public ReceiptData() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }   

    public String getBookingdate() {
        return bookingdate;
    }

    public void setBookingdate(String bookingdate) {
        this.bookingdate = bookingdate;
    }

    public String getSubservicebooked() {
        return subservicebooked;
    }

    public void setSubservicebooked(String subservicebooked) {
        this.subservicebooked = subservicebooked;
    }

    public String getVendor_name() {
        return vendor_name;
    }

    public void setVendor_name(String vendor_name) {
        this.vendor_name = vendor_name;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
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

    public double getTravelfee() {
        return travelfee;
    }

    public void setTravelfee(double travelfee) {
        this.travelfee = travelfee;
    }

    public double getTotalamount() {
        return totalamount;
    }

    public void setTotalamount(double totalamount) {
        this.totalamount = totalamount;
    }

    public double getPaymentpaid() {
        return paymentpaid;
    }

    public void setPaymentpaid(double paymentpaid) {
        this.paymentpaid = paymentpaid;
    }

    public String getPaymentmethod() {
        return paymentmethod;
    }

    public void setPaymentmethod(String paymentmethod) {
        this.paymentmethod = paymentmethod;
    }

    public String getPaymentdate() {
        return paymentdate;
    }

    public void setPaymentdate(String paymentdate) {
        this.paymentdate = paymentdate;
    }

    public String getPayment_reference() {
        return payment_reference;
    }

    public void setPayment_reference(String payment_reference) {
        this.payment_reference = payment_reference;
    }

    public double getDistancekm() {
        return distancekm;
    }

    public void setDistancekm(double distancekm) {
        this.distancekm = distancekm;
    }
}