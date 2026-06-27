package Model;

import java.util.List;

public class CustomerHomeData {
    private String status;
    private String customerName;
    private String customerImg;
    private String walletBalance;
    private int rewardsPoints;
    private List<Event> events;
    private List<Vendor> electricians;
    private List<Vendor> plumbers;
    private List<Vendor> lawns;

    public CustomerHomeData() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerImg() { return customerImg; }
    public void setCustomerImg(String customerImg) { this.customerImg = customerImg; }

    public String getWalletBalance() { return walletBalance; }
    public void setWalletBalance(String walletBalance) { this.walletBalance = walletBalance; }

    public int getRewardsPoints() { return rewardsPoints; }
    public void setRewardsPoints(int rewardsPoints) { this.rewardsPoints = rewardsPoints; }

    public List<Event> getEvents() { return events; }
    public void setEvents(List<Event> events) { this.events = events; }

    public List<Vendor> getElectricians() { return electricians; }
    public void setElectricians(List<Vendor> electricians) { this.electricians = electricians; }

    public List<Vendor> getPlumbers() { return plumbers; }
    public void setPlumbers(List<Vendor> plumbers) { this.plumbers = plumbers; }

    public List<Vendor> getLawns() { return lawns; }
    public void setLawns(List<Vendor> lawns) { this.lawns = lawns; }
}