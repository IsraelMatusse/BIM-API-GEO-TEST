package com.bim.api_test.integrations.geoApi.dtos.response;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingResultDto(
        Long id,
        String name,
        Double latitude,
        Double longitude,
        Double elevation,
        @JsonProperty("feature_code")
        String featureCode,
        @JsonProperty("country_code")
        String countryCode,
        @JsonProperty("admin1_id")
        Long admin1Id,
        @JsonProperty("admin2_id")
        Long admin2Id,
        String timezone,
        Long population,
        @JsonProperty("country_id")
        Long countryId,
        String country,
        String admin1,
        String admin2
) {
}

