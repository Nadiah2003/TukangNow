package Model;

public class PaymentRequestData {
    private int customerId;
    private int bookingId;
    private String paymentMethod;
    private String paymentType;
    private double amountOriginal;
    private double finalAmount;
    private int usePoints;
    private int customerVoucherId;
    private String service;
    private String category;
    private String vendorId;
    private int serviceId;
    private String emergencyServiceIds;
    private String problem;
    private String subservice;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String password;
    private String date;
    private String time;
    private double travelFee;
    private double distanceKm;
    private String evidencePath;

    public PaymentRequestData() {}

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public double getAmountOriginal() {
        return amountOriginal;
    }

    public void setAmountOriginal(double amountOriginal) {
        this.amountOriginal = amountOriginal;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public int getUsePoints() {
        return usePoints;
    }

    public void setUsePoints(int usePoints) {
        this.usePoints = usePoints;
    }

    public int getCustomerVoucherId() {
        return customerVoucherId;
    }

    public void setCustomerVoucherId(int customerVoucherId) {
        this.customerVoucherId = customerVoucherId;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public int getServiceId() {
        return serviceId;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    public String getEmergencyServiceIds() {
        return emergencyServiceIds;
    }

    public void setEmergencyServiceIds(String emergencyServiceIds) {
        this.emergencyServiceIds = emergencyServiceIds;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }

    public String getSubservice() {
        return subservice;
    }

    public void setSubservice(String subservice) {
        this.subservice = subservice;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getTravelFee() {
        return travelFee;
    }

    public void setTravelFee(double travelFee) {
        this.travelFee = travelFee;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getEvidencePath() {
        return evidencePath;
    }

    public void setEvidencePath(String evidencePath) {
        this.evidencePath = evidencePath;
    }
}