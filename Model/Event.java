package Model;

public class Event {
    private int id;
    private String title;
    private String description;
    private String img;
    private String discountCode;
    private int discountPercentage;
    private String startDate;
    private String endDate;
    private boolean isRedeemed;

    public Event() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImg() { return img; }
    public void setImg(String img) { this.img = img; }

    public String getDiscountCode() { return discountCode; }
    public void setDiscountCode(String discountCode) { this.discountCode = discountCode; }

    public int getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(int discountPercentage) { this.discountPercentage = discountPercentage; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public boolean isIsRedeemed() { return isRedeemed; }
    public void setIsRedeemed(boolean isRedeemed) { this.isRedeemed = isRedeemed; }
}