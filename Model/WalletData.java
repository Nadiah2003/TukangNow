package Model;

import java.util.ArrayList;
import java.util.List;

public class WalletData {
    private String status;
    private String message;
    private double balance;
    private List<WalletTransaction> transactions;

    public WalletData() {
        this.transactions = new ArrayList<>();
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

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public List<WalletTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<WalletTransaction> transactions) {
        this.transactions = transactions;
    }
}