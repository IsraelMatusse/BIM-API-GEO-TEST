package com.bim.api_test.integrations.wheatherApi.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WheatherDetailsResponse(
        Double latitude,
        Double longitude,
        @JsonProperty("generationtime_ms") Double generationTimeMs,
        @JsonProperty("utc_offset_seconds") Double utcOffsetSeconds,
        String timezone,
        @JsonProperty("timezone_abbreviation") String timezoneAbbreviation,
        Double elevation,
        @JsonProperty("current_units") WheatherCurrentUnitsResponse currentUnits,
        WheatherCurrentResponse current
) {
}
