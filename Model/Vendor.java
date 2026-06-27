package Model;

public class Vendor {
    private int id;
    private String name;
    private String email;
    private String nophone;
    private String password;
    private String docPath;
    private String profilePath;
    private String img;
    private String status;
    private String address;
    private String postcode;
    private String city;
    private String state;
    private double latitude;
    private double longitude;
    private double distanceKm;

    public Vendor() {}

    public Vendor(int id, String name, String profilePath) {
        this.id = id;
        this.name = name;
        this.profilePath = profilePath;
        this.img = profilePath;
    }

    public Vendor(int id, String name, String profilePath, String docPath) {
        this.id = id;
        this.name = name;
        this.profilePath = profilePath;
        this.img = profilePath;
        this.docPath = docPath;
    }

    public Vendor(int id, String name, String email, String nophone, String password, String docPath, String profilePath, String status, String address, String postcode, String city, String state, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.nophone = nophone;
        this.password = password;
        this.docPath = docPath;
        this.profilePath = profilePath;
        this.img = profilePath;
        this.status = status;
        this.address = address;
        this.postcode = postcode;
        this.city = city;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
    }

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

    public String getNophone() {
        return nophone;
    }

    public void setNophone(String nophone) {
        this.nophone = nophone;
    }   

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }   

    public String getDocPath() {
        return docPath;
    }

    public void setDocPath(String docPath) {
        this.docPath = docPath;
    }

    public String getDoc_path() {
        return docPath;
    }

    public void setDoc_path(String docPath) {
        this.docPath = docPath;
    }

    public String getProfilePath() {
        return profilePath;
    }

    public void setProfilePath(String profilePath) {
        this.profilePath = profilePath;
        this.img = profilePath;
    }

    public String getProfile_path() {
        return profilePath;
    }

    public void setProfile_path(String profilePath) {
        this.profilePath = profilePath;
        this.img = profilePath;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
        this.profilePath = img;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }   

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }   

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }   

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }   

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }   

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }   

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }
}