package Model;

import java.util.ArrayList;
import java.util.List;

public class PaymentInitialData {
    private String status;
    private String message;
    private String payment_type;
    private int rewards_points;
    private List<PaymentVoucher> vouchers;
    private double wallet_balance;

    public PaymentInitialData() {
        this.vouchers = new ArrayList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPayment_type() {
        return payment_type;
    }

    public void setPayment_type(String payment_type) {
        this.payment_type = payment_type;
    }

    public int getRewards_points() {
        return rewards_points;
    }

    public void setRewards_points(int rewards_points) {
        this.rewards_points = rewards_points;
    }

    public List<PaymentVoucher> getVouchers() {
        return vouchers;
    }

    public void setVouchers(List<PaymentVoucher> vouchers) {
        this.vouchers = vouchers;
    }

    public double getWallet_balance() {
        return wallet_balance;
    }

    public void setWallet_balance(double wallet_balance) {
        this.wallet_balance = wallet_balance;
    }
}