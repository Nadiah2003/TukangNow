package Model;

import java.util.ArrayList;
import java.util.List;

public class TrackingVendorData {
    private String status;
    private String message;
    private String viewer_role;
    private int booking_id;
    private int service_id;
    private String current_status;
    private String tracking_status;
    private String bookingdate;
    private boolean booking_date_reached;
    private boolean is_emergency;
    private String booking_service;
    private String subservicebooked;
    private String problem;
    private double deposit;
    private double totalamount;
    private double travelfee;
    private double materialcost;
    private double totalbalance;
    private String evidencepath;
    private double vendor_lat;
    private double vendor_lng;
    private double cust_lat;
    private double cust_lng;
    private String vendor_name;
    private String vendor_phone;
    private String profile_path;
    private String customer_name;
    private String customer_phone;
    private String customer_email;
    private String customer_profile_path;
    private String customer_address;
    private String eta;
    private String route_geometry;
    private String vendor_rating;
    private String duration_mins;
    private String distance_km;
    private String vehicle_model;
    private String plate_number;
    private String arrival_evidencepath;
    private String completion_evidencepath;
    private boolean has_rated;
    private String rating_val;
    private String rating_comment;
    private List<TrackingMaterialItem> materials;

    public TrackingVendorData() {
        this.materials = new ArrayList<>();
    }

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

    public String getViewer_role() {
        return viewer_role;
    }

    public void setViewer_role(String viewer_role) {
        this.viewer_role = viewer_role;
    }

    public int getBooking_id() {
        return booking_id;
    }

    public void setBooking_id(int booking_id) {
        this.booking_id = booking_id;
    }

    public int getService_id() {
        return service_id;
    }

    public void setService_id(int service_id) {
        this.service_id = service_id;
    }

    public String getCurrent_status() {
        return current_status;
    }

    public void setCurrent_status(String current_status) {
        this.current_status = current_status;
    }

    public String getTracking_status() {
        return tracking_status;
    }

    public void setTracking_status(String tracking_status) {
        this.tracking_status = tracking_status;
    }

    public String getBookingdate() {
        return bookingdate;
    }

    public void setBookingdate(String bookingdate) {
        this.bookingdate = bookingdate;
    }

    public boolean isBooking_date_reached() {
        return booking_date_reached;
    }

    public void setBooking_date_reached(boolean booking_date_reached) {
        this.booking_date_reached = booking_date_reached;
    }

    public boolean isIs_emergency() {
        return is_emergency;
    }

    public void setIs_emergency(boolean is_emergency) {
        this.is_emergency = is_emergency;
    }

    public String getBooking_service() {
        return booking_service;
    }

    public void setBooking_service(String booking_service) {
        this.booking_service = booking_service;
    }

    public String getSubservicebooked() {
        return subservicebooked;
    }

    public void setSubservicebooked(String subservicebooked) {
        this.subservicebooked = subservicebooked;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }

    public double getDeposit() {
        return deposit;
    }

    public void setDeposit(double deposit) {
        this.deposit = deposit;
    }

    public double getTotalamount() {
        return totalamount;
    }

    public void setTotalamount(double totalamount) {
        this.totalamount = totalamount;
    }

    public double getTravelfee() {
        return travelfee;
    }

    public void setTravelfee(double travelfee) {
        this.travelfee = travelfee;
    }

    public double getMaterialcost() {
        return materialcost;
    }

    public void setMaterialcost(double materialcost) {
        this.materialcost = materialcost;
    }

    public double getTotalbalance() {
        return totalbalance;
    }

    public void setTotalbalance(double totalbalance) {
        this.totalbalance = totalbalance;
    }

    public String getEvidencepath() {
        return evidencepath;
    }

    public void setEvidencepath(String evidencepath) {
        this.evidencepath = evidencepath;
    }

    public double getVendor_lat() {
        return vendor_lat;
    }

    public void setVendor_lat(double vendor_lat) {
        this.vendor_lat = vendor_lat;
    }

    public double getVendor_lng() {
        return vendor_lng;
    }

    public void setVendor_lng(double vendor_lng) {
        this.vendor_lng = vendor_lng;
    }

    public double getCust_lat() {
        return cust_lat;
    }

    public void setCust_lat(double cust_lat) {
        this.cust_lat = cust_lat;
    }

    public double getCust_lng() {
        return cust_lng;
    }

    public void setCust_lng(double cust_lng) {
        this.cust_lng = cust_lng;
    }

    public String getVendor_name() {
        return vendor_name;
    }

    public void setVendor_name(String vendor_name) {
        this.vendor_name = vendor_name;
    }

    public String getVendor_phone() {
        return vendor_phone;
    }

    public void setVendor_phone(String vendor_phone) {
        this.vendor_phone = vendor_phone;
    }

    public String getProfile_path() {
        return profile_path;
    }

    public void setProfile_path(String profile_path) {
        this.profile_path = profile_path;
    }

    public String getCustomer_name() {
        return customer_name;
    }

    public void setCustomer_name(String customer_name) {
        this.customer_name = customer_name;
    }

    public String getCustomer_phone() {
        return customer_phone;
    }

    public void setCustomer_phone(String customer_phone) {
        this.customer_phone = customer_phone;
    }

    public String getCustomer_email() {
        return customer_email;
    }

    public void setCustomer_email(String customer_email) {
        this.customer_email = customer_email;
    }

    public String getCustomer_profile_path() {
        return customer_profile_path;
    }

    public void setCustomer_profile_path(String customer_profile_path) {
        this.customer_profile_path = customer_profile_path;
    }

    public String getCustomer_address() {
        return customer_address;
    }

    public void setCustomer_address(String customer_address) {
        this.customer_address = customer_address;
    }

    public String getEta() {
        return eta;
    }

    public void setEta(String eta) {
        this.eta = eta;
    }

    public String getRoute_geometry() {
        return route_geometry;
    }

    public void setRoute_geometry(String route_geometry) {
        this.route_geometry = route_geometry;
    }

    public String getVendor_rating() {
        return vendor_rating;
    }

    public void setVendor_rating(String vendor_rating) {
        this.vendor_rating = vendor_rating;
    }

    public String getDuration_mins() {
        return duration_mins;
    }

    public void setDuration_mins(String duration_mins) {
        this.duration_mins = duration_mins;
    }

    public String getDistance_km() {
        return distance_km;
    }

    public void setDistance_km(String distance_km) {
        this.distance_km = distance_km;
    }

    public String getVehicle_model() {
        return vehicle_model;
    }

    public void setVehicle_model(String vehicle_model) {
        this.vehicle_model = vehicle_model;
    }

    public String getPlate_number() {
        return plate_number;
    }

    public void setPlate_number(String plate_number) {
        this.plate_number = plate_number;
    }

    public String getArrival_evidencepath() {
        return arrival_evidencepath;
    }

    public void setArrival_evidencepath(String arrival_evidencepath) {
        this.arrival_evidencepath = arrival_evidencepath;
    }

    public String getCompletion_evidencepath() {
        return completion_evidencepath;
    }

    public void setCompletion_evidencepath(String completion_evidencepath) {
        this.completion_evidencepath = completion_evidencepath;
    }

    public boolean isHas_rated() {
        return has_rated;
    }

    public void setHas_rated(boolean has_rated) {
        this.has_rated = has_rated;
    }

    public String getRating_val() {
        return rating_val;
    }

    public void setRating_val(String rating_val) {
        this.rating_val = rating_val;
    }

    public String getRating_comment() {
        return rating_comment;
    }

    public void setRating_comment(String rating_comment) {
        this.rating_comment = rating_comment;
    }

    public List<TrackingMaterialItem> getMaterials() {
        return materials;
    }

    public void setMaterials(List<TrackingMaterialItem> materials) {
        this.materials = materials;
    }
}