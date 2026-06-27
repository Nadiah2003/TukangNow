package Model;

public class AdminUser {
    private int id;
    private String name;
    private String email;
    private int admin_level;

    public AdminUser() {}

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
}