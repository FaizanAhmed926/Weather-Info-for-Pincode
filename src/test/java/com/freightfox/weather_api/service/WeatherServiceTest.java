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

@ExtendWith(MockitoExtension.class) // JUnit 5 ke saath Mockito enable karta hai
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

    // Dummy Data for Tests
    private final String TEST_PINCODE = "411014";
    private final LocalDate TEST_DATE = LocalDate.of(2020, 10, 15);
    private PincodeEntity dummyPincodeEntity;
    private WeatherEntity dummyWeatherEntity;

    @BeforeEach
    void setUp() {
        // API Key inject kar rahe hain (kyunki @Value test mein nahi chalta)
        ReflectionTestUtils.setField(weatherService, "apiKey", "dummy-api-key");

        // Dummy Objects taiyaar kar rahe hain
        dummyPincodeEntity = new PincodeEntity(TEST_PINCODE, 18.52, 73.85);
        dummyWeatherEntity = new WeatherEntity(TEST_DATE, 25.5, "Clear Sky", dummyPincodeEntity);
    }

    /**
     * Test Case 1: Optimization Check
     * Agar data Database mein hai, toh External API call NAHI honi chahiye.
     */
    @Test
    void testGetWeatherInfo_WhenDataInDB_ShouldReturnFromDB_AndNotCallAPI() {
        // 1. Mock Behavior Define karo (Scenario: Data DB mein hai)
        when(weatherRepository.findByPincodeEntity_PincodeAndForDate(TEST_PINCODE, TEST_DATE))
                .thenReturn(Optional.of(dummyWeatherEntity));

        // 2. Service Method Call karo
        WeatherEntity result = weatherService.getWeatherInfo(TEST_PINCODE, TEST_DATE);

        // 3. Verify karo
        assertNotNull(result);
        assertEquals(25.5, result.getTemperature());
        assertEquals("Clear Sky", result.getDescription());

        // CRITICAL CHECK: Verify karo ki API call ZERO baar hui hai
        verify(restTemplate, times(0)).getForObject(anyString(), any());
        verify(pincodeRepository, times(0)).findByPincode(anyString());

        System.out.println("Test 1 Passed: Data DB se aaya, API call bach gayi! ✅");
    }

    /**
     * Test Case 2: New Data Fetch
     * Agar data Database mein NAHI hai, toh External API call HONI chahiye aur Save hona chahiye.
     */
    @Test
    void testGetWeatherInfo_WhenDataNotInDB_ShouldCallAPI_AndSaveToDB() throws Exception {
        // 1. Mock Behavior (Scenario: Data DB mein nahi hai, lekin Pincode DB mein hai)
        when(weatherRepository.findByPincodeEntity_PincodeAndForDate(TEST_PINCODE, TEST_DATE))
                .thenReturn(Optional.empty()); // Weather nahi mila

        when(pincodeRepository.findByPincode(TEST_PINCODE))
                .thenReturn(Optional.of(dummyPincodeEntity)); // Pincode mil gaya

        // Mock External API Response
        String mockApiResponse = "{\"weather\":[{\"description\":\"Rain\"}], \"main\":{\"temp\":22.0}}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockApiResponse);

        // Mock Jackson Parsing
        JsonNode mockRootNode = Mockito.mock(JsonNode.class);
        JsonNode weatherArrayNode = Mockito.mock(JsonNode.class);
        JsonNode weatherNode = Mockito.mock(JsonNode.class);
        JsonNode mainNode = Mockito.mock(JsonNode.class);
        JsonNode descNode = Mockito.mock(JsonNode.class);
        JsonNode tempNode = Mockito.mock(JsonNode.class);

        // Jackson mocking thoda complex hota hai, hum behavior set kar rahe hain
        when(objectMapper.readTree(mockApiResponse)).thenReturn(mockRootNode);
        when(mockRootNode.path("weather")).thenReturn(weatherArrayNode);
        when(weatherArrayNode.get(0)).thenReturn(weatherNode);
        when(weatherNode.path("description")).thenReturn(descNode);
        when(descNode.asText()).thenReturn("Rain");

        when(mockRootNode.path("main")).thenReturn(mainNode);
        when(mainNode.path("temp")).thenReturn(tempNode);
        when(tempNode.asDouble()).thenReturn(22.0);

        // Save method mock (Return karega jo save kiya)
        when(weatherRepository.save(any(WeatherEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        // 2. Service Call
        WeatherEntity result = weatherService.getWeatherInfo(TEST_PINCODE, TEST_DATE);

        // 3. Verify
        assertEquals("Rain", result.getDescription());
        assertEquals(22.0, result.getTemperature());

        // CRITICAL CHECK: Verify karo ki API call EK baar hui hai
        verify(restTemplate, times(1)).getForObject(anyString(), any());
        // Verify karo ki Save method call hua hai
        verify(weatherRepository, times(1)).save(any(WeatherEntity.class));

        System.out.println("Test 2 Passed: API Call hui aur Data Save hua! ✅");
    }
}