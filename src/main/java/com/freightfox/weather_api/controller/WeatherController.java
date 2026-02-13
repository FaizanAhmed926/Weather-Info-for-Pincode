package com.freightfox.weather_api.controller;

import com.freightfox.weather_api.model.WeatherEntity;
import com.freightfox.weather_api.service.WeatherService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather")
    public ResponseEntity<?> getWeather(
            @RequestParam(name = "pincode") String pincode,

            @RequestParam(name = "for_date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate forDate
    ) {
        try {
            if (pincode == null || pincode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Error: Pincode cannot be empty");
            }

            WeatherEntity weatherData = weatherService.getWeatherInfo(pincode, forDate);

            return ResponseEntity.ok(weatherData);

        } catch (Exception e) {

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}