package com.freightfox.weather_api.repository;

import com.freightfox.weather_api.model.WeatherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WeatherRepository extends JpaRepository<WeatherEntity, Long> {

    // ✅ FIXED METHOD:
    // Humne beech mein '_' (underscore) lagaya hai.
    // Iska matlab Spring Boot samjhega: "PincodeEntity ke andar jao, aur uska 'pincode' field check karo"
    // Ab ye 'String' pincode accept karega.
    Optional<WeatherEntity> findByPincodeEntity_PincodeAndForDate(String pincode, LocalDate forDate);
}