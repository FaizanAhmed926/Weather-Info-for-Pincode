package com.freightfox.weather_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "pincode_details")
public class PincodeEntity {

    @Id
    @Column(name = "pincode", nullable = false, unique = true, length = 10)
    private String pincode; // Example: "411014"

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // --- Constructors ---

    // Empty Constructor (Required by JPA)
    public PincodeEntity() {
    }

    // All Arguments Constructor
    public PincodeEntity(String pincode, Double latitude, Double longitude) {
        this.pincode = pincode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // --- Getters and Setters (Manual) ---

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}