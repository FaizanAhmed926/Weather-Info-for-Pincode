package com.freightfox.weather_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "weather_info")
public class WeatherEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "for_date", nullable = false)
    private LocalDate forDate; // Example: 2020-10-15

    private Double temperature;

    private String description;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pincode_id", referencedColumnName = "pincode", nullable = false)
    @JsonIgnore
    private PincodeEntity pincodeEntity;


    public WeatherEntity() {
    }

    public WeatherEntity(LocalDate forDate, Double temperature, String description, PincodeEntity pincodeEntity) {
        this.forDate = forDate;
        this.temperature = temperature;
        this.description = description;
        this.pincodeEntity = pincodeEntity;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getForDate() {
        return forDate;
    }

    public void setForDate(LocalDate forDate) {
        this.forDate = forDate;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PincodeEntity getPincodeEntity() {
        return pincodeEntity;
    }

    public void setPincodeEntity(PincodeEntity pincodeEntity) {
        this.pincodeEntity = pincodeEntity;
    }
}