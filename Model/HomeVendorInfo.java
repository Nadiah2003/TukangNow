package Model;

public class HomeVendorInfo {
    private String name;
    private String accountStatus;
    private String profileImage;
    private int isFirstLogin;
    private String expiryInfo;
    private String walletBalance;
    private int totalFinish;
    private double avgRating;
    private String qualityScore;

    public HomeVendorInfo() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public int getIsFirstLogin() {
        return isFirstLogin;
    }

    public void setIsFirstLogin(int isFirstLogin) {
        this.isFirstLogin = isFirstLogin;
    }

    public String getExpiryInfo() {
        return expiryInfo;
    }

    public void setExpiryInfo(String expiryInfo) {
        this.expiryInfo = expiryInfo;
    }

    public String getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(String walletBalance) {
        this.walletBalance = walletBalance;
    }

    public int getTotalFinish() {
        return totalFinish;
    }

    public void setTotalFinish(int totalFinish) {
        this.totalFinish = totalFinish;
    }

    public double getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(double avgRating) {
        this.avgRating = avgRating;
    }

    public String getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(String qualityScore) {
        this.qualityScore = qualityScore;
    }
}