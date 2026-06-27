package Model;

public class VendorListItem {
    private int id;
    private String name;
    private String profile_path;
    private double depositPrice;
    private double rating;
    private int reviewCount;
    private double distance;

    public VendorListItem() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }   

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }   

    public String getProfile_path() {
        return profile_path;
    }

    public void setProfile_path(String profile_path) {
        this.profile_path = profile_path;
    }

    public double getDepositPrice() {
        return depositPrice;
    }

    public void setDepositPrice(double depositPrice) {
        this.depositPrice = depositPrice;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}