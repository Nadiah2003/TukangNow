package Model;

import java.util.ArrayList;
import java.util.List;

public class ProfileVendorInfo {
    private String name;
    private String email;
    private String nophone;
    private String address;
    private String postcode;
    private String city;
    private String state;
    private String profile_path;
    private String fullDisplayAddress;

    public ProfileVendorInfo() {}

    public void buildFullDisplayAddress() {
        List<String> parts = new ArrayList<>();

        if (address != null && !address.trim().isEmpty()) {
            parts.add(address.trim());
        }

        if (postcode != null && !postcode.trim().isEmpty()) {
            parts.add(postcode.trim());
        }

        if (city != null && !city.trim().isEmpty()) {
            parts.add(city.trim());
        }

        if (state != null && !state.trim().isEmpty()) {
            parts.add(state.trim());
        }

        if (parts.isEmpty()) {
            fullDisplayAddress = "No address set";
        } else {
            fullDisplayAddress = String.join(", ", parts);
        }
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

    public String getProfile_path() {
        return profile_path;
    }

    public void setProfile_path(String profile_path) {
        this.profile_path = profile_path;
    }

    public String getFullDisplayAddress() {
        return fullDisplayAddress;
    }

    public void setFullDisplayAddress(String fullDisplayAddress) {
        this.fullDisplayAddress = fullDisplayAddress;
    }
}