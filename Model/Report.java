package Model;

public class Report {
    private int id;
    private String reporterType;
    private int reporterId;
    private String reporterName;
    private String reportedType;
    private int reportedId;
    private String reportedName;
    private String reportedAccountStatus;
    private String reportOption;
    private String explanation;
    private String status;
    private String adminNote;
    private String actionTaken;
    private int actionAdminId;
    private String actionDate;
    private String createdAt;

    public Report() {
    }

    public Report(String reporterType, int reporterId, String reportedType, int reportedId, String reportOption, String explanation) {
        this.reporterType = reporterType;
        this.reporterId = reporterId;
        this.reportedType = reportedType;
        this.reportedId = reportedId;
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


    public String getReportedAccountStatus() {
        return reportedAccountStatus;
    }

    public void setReportedAccountStatus(String reportedAccountStatus) {
        this.reportedAccountStatus = reportedAccountStatus;
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
}