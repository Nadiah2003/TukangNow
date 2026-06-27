package Model;

public class ChatInfo {
    private String status;
    private String message;
    private int booking_id;
    private String viewer_role;
    private String partner_name;

    public ChatInfo() {}

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

    public int getBooking_id() {
        return booking_id;
    }

    public void setBooking_id(int booking_id) {
        this.booking_id = booking_id;
    }

    public String getViewer_role() {
        return viewer_role;
    }

    public void setViewer_role(String viewer_role) {
        this.viewer_role = viewer_role;
    }

    public String getPartner_name() {
        return partner_name;
    }

    public void setPartner_name(String partner_name) {
        this.partner_name = partner_name;
    }
}