package Model;

public class Customer {
    private int id;
    private String name;
    private String email;
    private String nophone;
    private String password;
    private String address;
    private String postcode;
    private String city;
    private String state;
    private double latitude;
    private double longitude;

    public Customer() {}

    public Customer(String name, String email, String nophone, String password) {
        this.name = name;
        this.email = email;
        this.nophone = nophone;
        this.password = password;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNophone() { return nophone; }
    public void setNophone(String nophone) { this.nophone = nophone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    // Address properties encapsulation configurations
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPostcode() { return postcode; }
    public void setPostcode(String postcode) { this.postcode = postcode; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}