package Model;

public class ChatMessage {
    private int id;
    private int booking_id;
    private int sender_id;
    private String sender_type;
    private String message;
    private String edited_message;
    private int is_edited;
    private int is_deleted;
    private String time_sent;
    private boolean can_action;

    public ChatMessage() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }   

    public int getBooking_id() {
        return booking_id;
    }

    public void setBooking_id(int booking_id) {
        this.booking_id = booking_id;
    }

    public int getSender_id() {
        return sender_id;
    }

    public void setSender_id(int sender_id) {
        this.sender_id = sender_id;
    }

    public String getSender_type() {
        return sender_type;
    }

    public void setSender_type(String sender_type) {
        this.sender_type = sender_type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEdited_message() {
        return edited_message;
    }

    public void setEdited_message(String edited_message) {
        this.edited_message = edited_message;
    }

    public int getIs_edited() {
        return is_edited;
    }

    public void setIs_edited(int is_edited) {
        this.is_edited = is_edited;
    }

    public int getIs_deleted() {
        return is_deleted;
    }

    public void setIs_deleted(int is_deleted) {
        this.is_deleted = is_deleted;
    }

    public String getTime_sent() {
        return time_sent;
    }

    public void setTime_sent(String time_sent) {
        this.time_sent = time_sent;
    }

    public boolean isCan_action() {
        return can_action;
    }

    public void setCan_action(boolean can_action) {
        this.can_action = can_action;
    }
}