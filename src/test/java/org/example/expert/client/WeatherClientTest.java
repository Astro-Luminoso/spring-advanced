package org.example.expert.client;

import org.example.expert.client.dto.WeatherDto;
import org.example.expert.domain.common.exception.ServerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WeatherClientTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;
    @Mock
    private RestTemplate restTemplate;

    @Test
    void getTodayWeather는_오늘_날짜와_일치하는_날씨를_반환한다() {
        WeatherClient weatherClient = newWeatherClient();
        WeatherDto[] weatherDtos = {
                new WeatherDto("01-01", "Snow"),
                new WeatherDto(today(), "Sunny")
        };
        given(restTemplate.getForEntity(any(URI.class), eq(WeatherDto[].class)))
                .willReturn(new ResponseEntity<>(weatherDtos, HttpStatus.OK));

        String weather = weatherClient.getTodayWeather();

        assertEquals("Sunny", weather);
    }

    @Test
    void getTodayWeather는_응답_상태가_OK가_아니면_ServerException을_던진다() {
        WeatherClient weatherClient = newWeatherClient();
        given(restTemplate.getForEntity(any(URI.class), eq(WeatherDto[].class)))
                .willReturn(new ResponseEntity<>(new WeatherDto[0], HttpStatus.INTERNAL_SERVER_ERROR));

        ServerException exception = assertThrows(ServerException.class, weatherClient::getTodayWeather);

        assertEquals("날씨 데이터를 가져오는데 실패했습니다. 상태 코드: 500 INTERNAL_SERVER_ERROR", exception.getMessage());
    }

    @Test
    void getTodayWeather는_body가_null이면_ServerException을_던진다() {
        WeatherClient weatherClient = newWeatherClient();
        given(restTemplate.getForEntity(any(URI.class), eq(WeatherDto[].class)))
                .willReturn(new ResponseEntity<>(null, HttpStatus.OK));

        ServerException exception = assertThrows(ServerException.class, weatherClient::getTodayWeather);

        assertEquals("날씨 데이터가 없습니다.", exception.getMessage());
    }

    @Test
    void getTodayWeather는_body가_비어있으면_ServerException을_던진다() {
        WeatherClient weatherClient = newWeatherClient();
        given(restTemplate.getForEntity(any(URI.class), eq(WeatherDto[].class)))
                .willReturn(new ResponseEntity<>(new WeatherDto[0], HttpStatus.OK));

        ServerException exception = assertThrows(ServerException.class, weatherClient::getTodayWeather);

        assertEquals("날씨 데이터가 없습니다.", exception.getMessage());
    }

    @Test
    void getTodayWeather는_오늘_날짜의_데이터가_없으면_ServerException을_던진다() {
        WeatherClient weatherClient = newWeatherClient();
        given(restTemplate.getForEntity(any(URI.class), eq(WeatherDto[].class)))
                .willReturn(new ResponseEntity<>(new WeatherDto[]{new WeatherDto("01-01", "Snow")}, HttpStatus.OK));

        ServerException exception = assertThrows(ServerException.class, weatherClient::getTodayWeather);

        assertEquals("오늘에 해당하는 날씨 데이터를 찾을 수 없습니다.", exception.getMessage());
    }

    private WeatherClient newWeatherClient() {
        given(restTemplateBuilder.build()).willReturn(restTemplate);
        return new WeatherClient(restTemplateBuilder);
    }

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
    }
}
