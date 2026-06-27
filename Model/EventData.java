package Model;

public class EventData {
    private String title;
    private String description;
    private String image_path;
    private String discount_code;
    private double discount_percentage;
    private String start_date;
    private String end_date;
    private String status;
    private int admin_id;

    public EventData() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }   

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }   

    public String getImage_path() {
        return image_path;
    }

    public void setImage_path(String image_path) {
        this.image_path = image_path;
    }   

    public String getDiscount_code() {
        return discount_code;
    }

    public void setDiscount_code(String discount_code) {
        this.discount_code = discount_code;
    }   

    public double getDiscount_percentage() {
        return discount_percentage;
    }

    public void setDiscount_percentage(double discount_percentage) {
        this.discount_percentage = discount_percentage;
    }   

    public String getStart_date() {
        return start_date;
    }

    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }   

    public String getEnd_date() {
        return end_date;
    }

    public void setEnd_date(String end_date) {
        this.end_date = end_date;
    }   

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }   

    public int getAdmin_id() {
        return admin_id;
    }

    public void setAdmin_id(int admin_id) {
        this.admin_id = admin_id;
    }
}