package Model;

public class CustomerVoucherItem {
    private int id;
    private String redeemed_at;
    private String voucher_name;
    private double discount_amount;

    public CustomerVoucherItem() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }   

    public String getRedeemed_at() {
        return redeemed_at;
    }

    public void setRedeemed_at(String redeemed_at) {
        this.redeemed_at = redeemed_at;
    }

    public String getVoucher_name() {
        return voucher_name;
    }

    public void setVoucher_name(String voucher_name) {
        this.voucher_name = voucher_name;
    }

    public double getDiscount_amount() {
        return discount_amount;
    }

    public void setDiscount_amount(double discount_amount) {
        this.discount_amount = discount_amount;
    }
}