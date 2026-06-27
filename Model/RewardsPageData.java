package Model;

import java.util.ArrayList;
import java.util.List;

public class RewardsPageData {
    private String status;
    private String message;
    private RewardsCustomer customer;
    private List<RewardCatalogItem> rewards;
    private List<CustomerVoucherItem> my_vouchers;
    private List<PointHistoryItem> history_vouchers;

    public RewardsPageData() {
        rewards = new ArrayList<>();
        my_vouchers = new ArrayList<>();
        history_vouchers = new ArrayList<>();
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

    public RewardsCustomer getCustomer() {
        return customer;
    }

    public void setCustomer(RewardsCustomer customer) {
        this.customer = customer;
    }

    public List<RewardCatalogItem> getRewards() {
        return rewards;
    }

    public void setRewards(List<RewardCatalogItem> rewards) {
        this.rewards = rewards;
    }

    public List<CustomerVoucherItem> getMy_vouchers() {
        return my_vouchers;
    }

    public void setMy_vouchers(List<CustomerVoucherItem> my_vouchers) {
        this.my_vouchers = my_vouchers;
    }

    public List<PointHistoryItem> getHistory_vouchers() {
        return history_vouchers;
    }

    public void setHistory_vouchers(List<PointHistoryItem> history_vouchers) {
        this.history_vouchers = history_vouchers;
    }
}