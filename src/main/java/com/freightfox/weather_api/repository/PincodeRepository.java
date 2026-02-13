package com.freightfox.weather_api.repository;

import com.freightfox.weather_api.model.PincodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PincodeRepository extends JpaRepository<PincodeEntity, String> {

    // Custom method: Pincode se Lat/Long dhundne ke liye
    // Return Optional taaki null pointer exception se bacha ja sake (Robustness)
    Optional<PincodeEntity> findByPincode(String pincode);
}