package com.bim.api_test.support;

import com.bim.api_test.domain.entities.GeoWhetherHistory;
import com.bim.api_test.integrations.geoApi.dtos.response.GeocodingResultDto;
import com.bim.api_test.integrations.wheatherApi.dtos.WheatherCurrentResponse;
import com.bim.api_test.integrations.wheatherApi.dtos.WheatherCurrentUnitsResponse;
import com.bim.api_test.integrations.wheatherApi.dtos.WheatherDetailsResponse;

import java.time.Instant;

/** Shared test data. Keeps the 18-argument constructors out of the test classes. */
public final class Fixtures {

    /** 07:00 UTC, which is 09:00 in Africa/Maputo (UTC+2, no DST). */
    public static final Instant CONSULTED_AT = Instant.parse("2026-09-08T07:00:00Z");

    /** How GeoWheatherResponse renders CONSULTED_AT for a record whose timeZone is Africa/Maputo. */
    public static final String CONSULTED_AT_FORMATTED = "08-09-2026 09:00";

    public static final double MAPUTO_LATITUDE = -25.96553;
    public static final double MAPUTO_LONGITUDE = 32.58322;

    private Fixtures() {
    }

    public static GeocodingResultDto maputo() {
        return location("Maputo", "Mozambique", "MZ", "Maputo City",
                MAPUTO_LATITUDE, MAPUTO_LONGITUDE, "Africa/Maputo");
    }

    public static GeocodingResultDto lisbon() {
        return location("Lisboa", "Portugal", "PT", "Lisboa",
                38.71667, -9.13333, "Europe/Lisbon");
    }

    public static GeocodingResultDto location(String name, String country, String countryCode, String admin1,
                                              double latitude, double longitude, String timezone) {
        return new GeocodingResultDto(
                1L, name, latitude, longitude, 47.0,
                "PPLC", countryCode, 10L, 20L, timezone,
                1_000_000L, 30L, country, admin1, null);
    }

    public static WheatherDetailsResponse weather() {
        return new WheatherDetailsResponse(
                MAPUTO_LATITUDE, MAPUTO_LONGITUDE, 1.5, 7200.0, "Africa/Maputo", "CAT", 47.0,
                new WheatherCurrentUnitsResponse("iso8601", "seconds", "C", "%", "C", "wmo code", "km/h"),
                new WheatherCurrentResponse("2026-09-08T09:00", 900.0, 27.4, 68, 29.1, 3, 11.2));
    }

    /** The entity as it looks once persisted: with an id and a fixed consultedAt. */
    public static GeoWhetherHistory savedHistory(String id) {
        GeoWhetherHistory history = new GeoWhetherHistory(maputo(), weather());
        history.setId(id);
        history.setConsultedAt(CONSULTED_AT);
        return history;
    }
}
