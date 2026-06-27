package Model;

public class VendorWallet {
    private int vendorId;
    private double balance;
    private double totalEarned;
    private double totalWithdrawn;

    public VendorWallet() {
    }

    public int getVendorId() {
        return vendorId;
    }

    public void setVendorId(int vendorId) {
        this.vendorId = vendorId;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getTotalEarned() {
        return totalEarned;
    }

    public void setTotalEarned(double totalEarned) {
        this.totalEarned = totalEarned;
    }

    public double getTotalWithdrawn() {
        return totalWithdrawn;
    }

    public void setTotalWithdrawn(double totalWithdrawn) {
        this.totalWithdrawn = totalWithdrawn;
    }
}