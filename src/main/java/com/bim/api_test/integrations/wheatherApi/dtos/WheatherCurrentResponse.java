package com.bim.api_test.integrations.wheatherApi.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WheatherCurrentResponse(
        String time,
        Double interval,
        @JsonProperty("temperature_2m") Double temperature2m,
        @JsonProperty("relative_humidity_2m") Integer relativeHumidity2m,
        @JsonProperty("apparent_temperature") Double apparentTemperature,
        @JsonProperty("weather_code") Integer weatherCode,
        @JsonProperty("wind_speed_10m") Double windSpeed10m
) {
}
