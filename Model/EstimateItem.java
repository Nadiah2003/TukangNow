package Model;

public class EstimateItem {
    private int id;
    private String serviceKeyword;
    private String itemName;
    private double itemPrice;

    public EstimateItem() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }   

    public String getServiceKeyword() {
        return serviceKeyword;
    }

    public void setServiceKeyword(String serviceKeyword) {
        this.serviceKeyword = serviceKeyword;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(double itemPrice) {
        this.itemPrice = itemPrice;
    }
}