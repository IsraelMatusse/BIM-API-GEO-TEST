package com.bim.api_test.integrations.wheatherApi.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WheatherCurrentUnitsResponse(
        String time,
        String interval,
        @JsonProperty("temperature_2m") String temperature2m,
        @JsonProperty("relative_humidity_2m") String relativeHumidity2m,
        @JsonProperty("apparent_temperature") String apparentTemperature,
        @JsonProperty("weather_code") String weatherCode,
        @JsonProperty("wind_speed_10m") String windSpeed10m
) {
}
