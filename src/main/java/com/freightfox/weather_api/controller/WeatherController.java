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

    // Constructor Injection (Best Practice)
    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * Endpoint: GET /api/weather
     * Example: /api/weather?pincode=411014&for_date=2020-10-15
     */
    @GetMapping("/weather")
    public ResponseEntity<?> getWeather(
            @RequestParam(name = "pincode") String pincode,

            @RequestParam(name = "for_date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate forDate
    ) {
        try {
            // Validation: Pincode khali nahi hona chahiye
            if (pincode == null || pincode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Error: Pincode cannot be empty");
            }

            // Service call logic
            WeatherEntity weatherData = weatherService.getWeatherInfo(pincode, forDate);

            return ResponseEntity.ok(weatherData);

        } catch (Exception e) {
            // Agar koi error aata hai (jaise API fail ho gayi, ya pincode galat hai)
            // toh hum server crash hone ke bajaye user ko readable error denge.
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}