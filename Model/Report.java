package Model;

public class Report {
    private int id;
    private int bookingId;
    private String reporterType;
    private int reporterId;
    private String reporterName;
    private String reporterEmail;
    private String reporterPhone;
    private String reporterAddress;
    private String reporterAccountStatus;
    private int reporterSuspendCount;
    private String reportedType;
    private int reportedId;
    private String reportedName;
    private String reportedEmail;
    private String reportedPhone;
    private String reportedAddress;
    private String reportedAccountStatus;
    private int reportedSuspendCount;
    private String reportedSuspendStartDate;
    private String reportedSuspendEndDate;
    private String reportedBanReason;
    private String reportOption;
    private String explanation;
    private String status;
    private String adminNote;
    private String actionTaken;
    private int actionAdminId;
    private String actionDate;
    private String createdAt;
    private String bookingDate;
    private String subserviceBooked;
    private String problem;
    private String bookingStatus;
    private double deposit;
    private double totalAmount;
    private double travelFee;
    private double materialCost;
    private double totalBalance;
    private double distanceKm;
    private String evidencePath;

    public Report() {
    }

    public Report(String reporterType, int reporterId, String reportedType, int reportedId, int bookingId, String reportOption, String explanation) {
        this.reporterType = reporterType;
        this.reporterId = reporterId;
        this.reportedType = reportedType;
        this.reportedId = reportedId;
        this.bookingId = bookingId;
        this.reportOption = reportOption;
        this.explanation = explanation;
        this.status = "Submitted";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }


    public String getReporterType() {
        return reporterType;
    }

    public void setReporterType(String reporterType) {
        this.reporterType = reporterType;
    }


    public int getReporterId() {
        return reporterId;
    }

    public void setReporterId(int reporterId) {
        this.reporterId = reporterId;
    }


    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }


    public String getReporterEmail() {
        return reporterEmail;
    }

    public void setReporterEmail(String reporterEmail) {
        this.reporterEmail = reporterEmail;
    }


    public String getReporterPhone() {
        return reporterPhone;
    }

    public void setReporterPhone(String reporterPhone) {
        this.reporterPhone = reporterPhone;
    }


    public String getReporterAddress() {
        return reporterAddress;
    }

    public void setReporterAddress(String reporterAddress) {
        this.reporterAddress = reporterAddress;
    }


    public String getReporterAccountStatus() {
        return reporterAccountStatus;
    }

    public void setReporterAccountStatus(String reporterAccountStatus) {
        this.reporterAccountStatus = reporterAccountStatus;
    }


    public int getReporterSuspendCount() {
        return reporterSuspendCount;
    }

    public void setReporterSuspendCount(int reporterSuspendCount) {
        this.reporterSuspendCount = reporterSuspendCount;
    }


    public String getReportedType() {
        return reportedType;
    }

    public void setReportedType(String reportedType) {
        this.reportedType = reportedType;
    }


    public int getReportedId() {
        return reportedId;
    }

    public void setReportedId(int reportedId) {
        this.reportedId = reportedId;
    }


    public String getReportedName() {
        return reportedName;
    }

    public void setReportedName(String reportedName) {
        this.reportedName = reportedName;
    }


    public String getReportedEmail() {
        return reportedEmail;
    }

    public void setReportedEmail(String reportedEmail) {
        this.reportedEmail = reportedEmail;
    }


    public String getReportedPhone() {
        return reportedPhone;
    }

    public void setReportedPhone(String reportedPhone) {
        this.reportedPhone = reportedPhone;
    }


    public String getReportedAddress() {
        return reportedAddress;
    }

    public void setReportedAddress(String reportedAddress) {
        this.reportedAddress = reportedAddress;
    }


    public String getReportedAccountStatus() {
        return reportedAccountStatus;
    }

    public void setReportedAccountStatus(String reportedAccountStatus) {
        this.reportedAccountStatus = reportedAccountStatus;
    }


    public int getReportedSuspendCount() {
        return reportedSuspendCount;
    }

    public void setReportedSuspendCount(int reportedSuspendCount) {
        this.reportedSuspendCount = reportedSuspendCount;
    }


    public String getReportedSuspendStartDate() {
        return reportedSuspendStartDate;
    }

    public void setReportedSuspendStartDate(String reportedSuspendStartDate) {
        this.reportedSuspendStartDate = reportedSuspendStartDate;
    }


    public String getReportedSuspendEndDate() {
        return reportedSuspendEndDate;
    }

    public void setReportedSuspendEndDate(String reportedSuspendEndDate) {
        this.reportedSuspendEndDate = reportedSuspendEndDate;
    }


    public String getReportedBanReason() {
        return reportedBanReason;
    }

    public void setReportedBanReason(String reportedBanReason) {
        this.reportedBanReason = reportedBanReason;
    }


    public String getReportOption() {
        return reportOption;
    }

    public void setReportOption(String reportOption) {
        this.reportOption = reportOption;
    }


    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }


    public String getActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }


    public int getActionAdminId() {
        return actionAdminId;
    }

    public void setActionAdminId(int actionAdminId) {
        this.actionAdminId = actionAdminId;
    }


    public String getActionDate() {
        return actionDate;
    }

    public void setActionDate(String actionDate) {
        this.actionDate = actionDate;
    }


    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }


    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }


    public String getSubserviceBooked() {
        return subserviceBooked;
    }

    public void setSubserviceBooked(String subserviceBooked) {
        this.subserviceBooked = subserviceBooked;
    }


    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }


    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }


    public double getDeposit() {
        return deposit;
    }

    public void setDeposit(double deposit) {
        this.deposit = deposit;
    }


    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }


    public double getTravelFee() {
        return travelFee;
    }

    public void setTravelFee(double travelFee) {
        this.travelFee = travelFee;
    }


    public double getMaterialCost() {
        return materialCost;
    }

    public void setMaterialCost(double materialCost) {
        this.materialCost = materialCost;
    }


    public double getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(double totalBalance) {
        this.totalBalance = totalBalance;
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