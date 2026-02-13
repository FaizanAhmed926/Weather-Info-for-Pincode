package com.freightfox.weather_api.repository;

import com.freightfox.weather_api.model.PincodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PincodeRepository extends JpaRepository<PincodeEntity, String> {

    Optional<PincodeEntity> findByPincode(String pincode);
}