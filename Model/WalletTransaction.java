package Model;

public class WalletTransaction {
    private double amount;
    private String type;
    private String reference_id;
    private String created_at;
    private String date;
    private String description;

    public WalletTransaction() {}

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReference_id() {
        return reference_id;
    }

    public void setReference_id(String reference_id) {
        this.reference_id = reference_id;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }   

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}