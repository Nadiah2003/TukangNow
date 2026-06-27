package Model;

public class RewardCatalogItem {
    private int id;
    private String voucher_name;
    private int points_required;
    private double discount_amount;

    public RewardCatalogItem() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }   

    public String getVoucher_name() {
        return voucher_name;
    }

    public void setVoucher_name(String voucher_name) {
        this.voucher_name = voucher_name;
    }

    public int getPoints_required() {
        return points_required;
    }

    public void setPoints_required(int points_required) {
        this.points_required = points_required;
    }

    public double getDiscount_amount() {
        return discount_amount;
    }

    public void setDiscount_amount(double discount_amount) {
        this.discount_amount = discount_amount;
    }
}