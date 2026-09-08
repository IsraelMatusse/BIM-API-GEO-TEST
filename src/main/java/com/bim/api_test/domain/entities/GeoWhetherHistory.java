package com.bim.api_test.domain.entities;

import com.bim.api_test.integrations.geoApi.dtos.response.GeocodingResultDto;
import com.bim.api_test.integrations.wheatherApi.dtos.WheatherDetailsResponse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "geo_weather_history")
public class GeoWhetherHistory extends  BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "country")
    private String country;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "region")
    private String region;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "time_zone")
    private String timeZone;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "temperature_unit")
    private String temperatureUnit;

    @Column(name = "apparent_temperature")
    private Double apparentTemperature;

    @Column(name = "humidity")
    private Integer humidity;

    @Column(name = "humidity_unit")
    private String humidityUnit;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Column(name = "wind_speed_unit")
    private String windSpeedUnit;

    @Column(name = "weather_code")
    private Integer weatherCode;

    @Column(name = "weather_time")
    private String weatherTime;

    @Column(name = "consulted_at", nullable = false)
    private Instant consultedAt;

    public GeoWhetherHistory(GeocodingResultDto location, WheatherDetailsResponse weather){
        this.temperature = weather.current().temperature2m();
        this.apparentTemperature = weather.current().apparentTemperature();
        this.city=location.name();
        this.country=location.country();
        this.countryCode=location.countryCode();
        this.humidity=weather.current().relativeHumidity2m();
        this.longitude=location.longitude();
        this.latitude=location.latitude();
        this.timeZone=location.timezone();
        this.consultedAt= Instant.now();
        this.weatherTime=weather.current().time();
        this.weatherCode=weather.current().weatherCode();
        this.windSpeed= weather.current().windSpeed10m();
        this.windSpeedUnit= weather.currentUnits().windSpeed10m();
        this.humidityUnit=weather.currentUnits().relativeHumidity2m();
        this.temperatureUnit=weather.currentUnits().temperature2m();
        this.region= location.admin1();
    }

    public void delete (){
        this.setDeletedAt(LocalDateTime.now());
    }
}
