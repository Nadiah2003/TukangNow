package Model;

public class AdminProfile {
    private int id;
    private String name;
    private String email;
    private int admin_level;
    private String profile_path;

    public AdminProfile() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }   

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }   

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }   

    public int getAdmin_level() {
        return admin_level;
    }

    public void setAdmin_level(int admin_level) {
        this.admin_level = admin_level;
    }

    public String getProfile_path() {
        return profile_path;
    }

    public void setProfile_path(String profile_path) {
        this.profile_path = profile_path;
    }
}