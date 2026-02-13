package com.freightfox.weather_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freightfox.weather_api.model.PincodeEntity;
import com.freightfox.weather_api.model.WeatherEntity;
import com.freightfox.weather_api.repository.PincodeRepository;
import com.freightfox.weather_api.repository.WeatherRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class WeatherService {

    private final PincodeRepository pincodeRepository;
    private final WeatherRepository weatherRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper; // JSON parsing ke liye robust tool

    @Value("${weather.api.key}")
    private String apiKey;

    public WeatherService(PincodeRepository pincodeRepository,
                          WeatherRepository weatherRepository,
                          RestTemplate restTemplate,
                          ObjectMapper objectMapper) {
        this.pincodeRepository = pincodeRepository;
        this.weatherRepository = weatherRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional // Transaction fail hone par data rollback karega (Robustness)
    public WeatherEntity getWeatherInfo(String pincode, LocalDate forDate) {

        // --- STEP 1: OPTIMIZATION CHECK (Cache Hit) ---
        // Sabse pehle DB check karo. Agar data hai, toh API call bacha lo.
        Optional<WeatherEntity> cachedWeather = weatherRepository.findByPincodeEntity_PincodeAndForDate(pincode, forDate);
        if (cachedWeather.isPresent()) {
            System.out.println("Returning Weather Data from Database (No API Call)");
            return cachedWeather.get();
        }

        // --- STEP 2: LOCATION CHECK ---
        // Agar weather nahi mila, toh check karo kya Pincode ka Lat/Long hamare paas hai?
        PincodeEntity pincodeEntity = pincodeRepository.findByPincode(pincode)
                .orElseGet(() -> fetchAndSaveLatLong(pincode)); // Agar nahi hai, toh Geocoding API call karo

        // --- STEP 3: FETCH WEATHER FROM API ---
        // Ab hamare paas Lat/Long hai, OpenWeather API call karo
        WeatherEntity newWeather = fetchWeatherFromApi(pincodeEntity, forDate);

        // --- STEP 4: SAVE TO DB (Cache Miss -> Store) ---
        // Future calls ke liye data save karo
        return weatherRepository.save(newWeather);
    }

    // Helper Method 1: Geocoding API (Pincode -> Lat/Long)
    private PincodeEntity fetchAndSaveLatLong(String pincode) {
        System.out.println("Fetching Lat/Long from OpenWeather Geocoding API...");

        String url = UriComponentsBuilder.fromHttpUrl("http://api.openweathermap.org/geo/1.0/zip")
                .queryParam("zip", pincode + ",IN") // Assuming India
                .queryParam("appid", apiKey)
                .toUriString();

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            PincodeEntity entity = new PincodeEntity();
            entity.setPincode(pincode);
            entity.setLatitude(root.path("lat").asDouble());
            entity.setLongitude(root.path("lon").asDouble());

            return pincodeRepository.save(entity); // Lat/Long save kar diya
        } catch (Exception e) {
            throw new RuntimeException("Invalid Pincode or API Error: " + e.getMessage());
        }
    }

    // Helper Method 2: Weather API (Lat/Long -> Weather Info)
    private WeatherEntity fetchWeatherFromApi(PincodeEntity pincodeEntity, LocalDate forDate) {
        System.out.println("Fetching Weather from OpenWeather API...");

        // NOTE: Free OpenWeather API sirf 'Current' weather deta hai.
        // Assignment ki requirement puri karne ke liye hum current weather laayenge,
        // lekin use requested 'forDate' ke saath save karenge taaki caching logic test ho sake.

        String url = UriComponentsBuilder.fromHttpUrl("https://api.openweathermap.org/data/2.5/weather")
                .queryParam("lat", pincodeEntity.getLatitude())
                .queryParam("lon", pincodeEntity.getLongitude())
                .queryParam("units", "metric")
                .queryParam("appid", apiKey)
                .toUriString();

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            WeatherEntity weather = new WeatherEntity();
            weather.setPincodeEntity(pincodeEntity);
            weather.setForDate(forDate); // Requested date set kar rahe hain caching ke liye
            weather.setDescription(root.path("weather").get(0).path("description").asText());
            weather.setTemperature(root.path("main").path("temp").asDouble());

            return weather;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching weather data: " + e.getMessage());
        }
    }
}
