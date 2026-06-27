package Model;

public class VendorServiceDetail {
    private String status;
    private String message;
    private int id;
    private String name;
    private String img;
    private String service_name;
    private String subservice;
    private String startprice;
    private String avail_date;
    private String avail_time;
    private String rating;

    public VendorServiceDetail() {}

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

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }    

    public String getServiceName() {
        return service_name;
    }

    public void setServiceName(String service_name) {
        this.service_name = service_name;
    }    

    public String getSubservice() {
        return subservice;
    }

    public void setSubservice(String subservice) {
        this.subservice = subservice;
    }    

    public String getStartprice() {
        return startprice;
    }

    public void setStartprice(String startprice) {
        this.startprice = startprice;
    }    

    public String getAvailDate() {
        return avail_date;
    }

    public void setAvailDate(String avail_date) {
        this.avail_date = avail_date;
    }    

    public String getAvailTime() {
        return avail_time;
    }

    public void setAvailTime(String avail_time) {
        this.avail_time = avail_time;
    }    

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }
}