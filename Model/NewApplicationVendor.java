package Model;

public class NewApplicationVendor {
    private int id;
    private String name;
    private String nophone;
    private String profilePath;
    private String licenseUrl;

    public NewApplicationVendor() {}

    public NewApplicationVendor(int id, String name, String nophone, String profilePath, String licenseUrl) {
        this.id = id;
        this.name = name;
        this.nophone = nophone;
        this.profilePath = profilePath;
        this.licenseUrl = licenseUrl;
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

    public String getNophone() {
        return nophone;
    }

    public void setNophone(String nophone) {
        this.nophone = nophone;
    }    

    public String getProfilePath() {
        return profilePath;
    }

    public void setProfilePath(String profilePath) {
        this.profilePath = profilePath;
    }    

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }
}