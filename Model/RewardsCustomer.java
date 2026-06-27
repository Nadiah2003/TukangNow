package Model;

public class RewardsCustomer {
    private String name;
    private int rewards_points;

    public RewardsCustomer() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }   

    public int getRewards_points() {
        return rewards_points;
    }

    public void setRewards_points(int rewards_points) {
        this.rewards_points = rewards_points;
    }
}