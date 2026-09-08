package com.bim.api_test.integrations.geoApi.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingResponse(
        List<GeocodingResultDto> results,
        @JsonProperty("generationtime_ms") Double generationtimeMs
) {
}
