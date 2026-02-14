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
    private final ObjectMapper objectMapper;
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

    @Transactional
    public WeatherEntity getWeatherInfo(String pincode, LocalDate forDate) {

        Optional<WeatherEntity> cachedWeather = weatherRepository.findByPincodeEntity_PincodeAndForDate(pincode, forDate);
        if (cachedWeather.isPresent()) {
            System.out.println("Returning Weather Data from Database (No API Call)");
            return cachedWeather.get();
        }

        PincodeEntity pincodeEntity = pincodeRepository.findByPincode(pincode)
                .orElseGet(() -> fetchAndSaveLatLong(pincode));

        WeatherEntity newWeather = fetchWeatherFromApi(pincodeEntity, forDate);

        return weatherRepository.save(newWeather);
    }

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

            return pincodeRepository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Pincode or API Error: " + e.getMessage());
        }
    }

    private WeatherEntity fetchWeatherFromApi(PincodeEntity pincodeEntity, LocalDate forDate) {
        System.out.println("Fetching Weather from OpenWeather API...");

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
            weather.setForDate(forDate); 
            weather.setDescription(root.path("weather").get(0).path("description").asText());
            weather.setTemperature(root.path("main").path("temp").asDouble());

            return weather;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching weather data: " + e.getMessage());
        }
    }
}
