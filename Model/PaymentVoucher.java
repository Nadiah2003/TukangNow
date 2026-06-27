package Model;

public class PaymentVoucher {
    private int customer_voucher_id;
    private String event_name;
    private String voucher_code;
    private double discount_amount;

    public PaymentVoucher() {}

    public int getCustomer_voucher_id() {
        return customer_voucher_id;
    }

    public void setCustomer_voucher_id(int customer_voucher_id) {
        this.customer_voucher_id = customer_voucher_id;
    }

    public String getEvent_name() {
        return event_name;
    }

    public void setEvent_name(String event_name) {
        this.event_name = event_name;
    }

    public String getVoucher_code() {
        return voucher_code;
    }

    public void setVoucher_code(String voucher_code) {
        this.voucher_code = voucher_code;
    }

    public double getDiscount_amount() {
        return discount_amount;
    }

    public void setDiscount_amount(double discount_amount) {
        this.discount_amount = discount_amount;
    }
}