package Model;

public class TrackingMaterialItem {
    private int id;
    private int booking_id;
    private int receipt_id;
    private String receipt_label;
    private String item_name;
    private int quantity;
    private double price;
    private String receipt_path;

    public TrackingMaterialItem() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }   

    public int getBooking_id() {
        return booking_id;
    }

    public void setBooking_id(int booking_id) {
        this.booking_id = booking_id;
    }

    public int getReceipt_id() {
        return receipt_id;
    }

    public void setReceipt_id(int receipt_id) {
        this.receipt_id = receipt_id;
    }

    public String getReceipt_label() {
        return receipt_label;
    }

    public void setReceipt_label(String receipt_label) {
        this.receipt_label = receipt_label;
    }

    public String getItem_name() {
        return item_name;
    }

    public void setItem_name(String item_name) {
        this.item_name = item_name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }   

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }   

    public String getReceipt_path() {
        return receipt_path;
    }

    public void setReceipt_path(String receipt_path) {
        this.receipt_path = receipt_path;
    }
}