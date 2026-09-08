package com.bim.api_test.interfaces.dtos.response;


import com.bim.api_test.domain.entities.GeoWhetherHistory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public record GeoWheatherResponse(
        String id,
        String city,
        String country,
        String countryCode,
        String region,
        Double latitude,
        Double longitude,
        String timezone,
        Double temperature,
        String temperatureUnit,
        Double apparentTemperature,
        Integer humidity,
        String humidityUnit,
        Double windSpeed,
        String windSpeedUnit,
        Integer weatherCode,
        String weatherTime,
        String consultedAt
) {
   private static final DateTimeFormatter CONSULTED_AT_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

   public GeoWheatherResponse (GeoWhetherHistory geoWhetherHistory){
       this(
               geoWhetherHistory.getId(),
               geoWhetherHistory.getCity(),
               geoWhetherHistory.getCountry(),
               geoWhetherHistory.getCountryCode(),
               geoWhetherHistory.getRegion(),
               geoWhetherHistory.getLatitude(),
               geoWhetherHistory.getLongitude(),
               geoWhetherHistory.getTimeZone(),
               geoWhetherHistory.getTemperature(),
               geoWhetherHistory.getTemperatureUnit(),
               geoWhetherHistory.getApparentTemperature(),
               geoWhetherHistory.getHumidity(),
               geoWhetherHistory.getHumidityUnit(),
               geoWhetherHistory.getWindSpeed(),
               geoWhetherHistory.getWindSpeedUnit(),
               geoWhetherHistory.getWeatherCode(),
               geoWhetherHistory.getWeatherTime(),
               formatConsultedAt(geoWhetherHistory.getConsultedAt(), geoWhetherHistory.getTimeZone())
       );
   }

   private static String formatConsultedAt(Instant instant, String timeZone) {
       if (instant == null) {
           return null;
       }

       try {
           ZoneId zoneId = timeZone != null ? ZoneId.of(timeZone) : ZoneOffset.UTC;
           return instant.atZone(zoneId).format(CONSULTED_AT_FORMATTER);
       } catch (Exception ignored) {
           return instant.atZone(ZoneOffset.UTC).format(CONSULTED_AT_FORMATTER);
       }
   }
}
