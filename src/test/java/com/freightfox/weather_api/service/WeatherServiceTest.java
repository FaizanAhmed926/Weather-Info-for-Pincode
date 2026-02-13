package com.freightfox.weather_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freightfox.weather_api.model.PincodeEntity;
import com.freightfox.weather_api.model.WeatherEntity;
import com.freightfox.weather_api.repository.PincodeRepository;
import com.freightfox.weather_api.repository.WeatherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private PincodeRepository pincodeRepository;

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WeatherService weatherService;

    private final String TEST_PINCODE = "411014";
    private final LocalDate TEST_DATE = LocalDate.of(2020, 10, 15);
    private PincodeEntity dummyPincodeEntity;
    private WeatherEntity dummyWeatherEntity;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(weatherService, "apiKey", "dummy-api-key");

        dummyPincodeEntity = new PincodeEntity(TEST_PINCODE, 18.52, 73.85);
        dummyWeatherEntity = new WeatherEntity(TEST_DATE, 25.5, "Clear Sky", dummyPincodeEntity);
    }


    @Test
    void testGetWeatherInfo_WhenDataInDB_ShouldReturnFromDB_AndNotCallAPI() {
        when(weatherRepository.findByPincodeEntity_PincodeAndForDate(TEST_PINCODE, TEST_DATE))
                .thenReturn(Optional.of(dummyWeatherEntity));

        WeatherEntity result = weatherService.getWeatherInfo(TEST_PINCODE, TEST_DATE);

        assertNotNull(result);
        assertEquals(25.5, result.getTemperature());
        assertEquals("Clear Sky", result.getDescription());

        verify(restTemplate, times(0)).getForObject(anyString(), any());
        verify(pincodeRepository, times(0)).findByPincode(anyString());

        System.out.println("Test 1 Passed: Data DB se aaya, API call bach gayi! ✅");
    }

    @Test
    void testGetWeatherInfo_WhenDataNotInDB_ShouldCallAPI_AndSaveToDB() throws Exception {
        when(weatherRepository.findByPincodeEntity_PincodeAndForDate(TEST_PINCODE, TEST_DATE))
                .thenReturn(Optional.empty());

        when(pincodeRepository.findByPincode(TEST_PINCODE))
                .thenReturn(Optional.of(dummyPincodeEntity));


        String mockApiResponse = "{\"weather\":[{\"description\":\"Rain\"}], \"main\":{\"temp\":22.0}}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockApiResponse);

        JsonNode mockRootNode = Mockito.mock(JsonNode.class);
        JsonNode weatherArrayNode = Mockito.mock(JsonNode.class);
        JsonNode weatherNode = Mockito.mock(JsonNode.class);
        JsonNode mainNode = Mockito.mock(JsonNode.class);
        JsonNode descNode = Mockito.mock(JsonNode.class);
        JsonNode tempNode = Mockito.mock(JsonNode.class);

        when(objectMapper.readTree(mockApiResponse)).thenReturn(mockRootNode);
        when(mockRootNode.path("weather")).thenReturn(weatherArrayNode);
        when(weatherArrayNode.get(0)).thenReturn(weatherNode);
        when(weatherNode.path("description")).thenReturn(descNode);
        when(descNode.asText()).thenReturn("Rain");

        when(mockRootNode.path("main")).thenReturn(mainNode);
        when(mainNode.path("temp")).thenReturn(tempNode);
        when(tempNode.asDouble()).thenReturn(22.0);

        when(weatherRepository.save(any(WeatherEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        WeatherEntity result = weatherService.getWeatherInfo(TEST_PINCODE, TEST_DATE);

        assertEquals("Rain", result.getDescription());
        assertEquals(22.0, result.getTemperature());

        verify(restTemplate, times(1)).getForObject(anyString(), any());
        verify(weatherRepository, times(1)).save(any(WeatherEntity.class));

        System.out.println("Test 2 Passed: API Call hui aur Data Save hua! ✅");
    }
}