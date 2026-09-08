package com.bim.api_test.interfaces.controllers;

import com.bim.api_test.domain.services.GeoWheatherService;
import com.bim.api_test.infrastructure.exceptions.BadGatewayException;
import com.bim.api_test.infrastructure.exceptions.BadRequestException;
import com.bim.api_test.infrastructure.exceptions.InternalServerErrorException;
import com.bim.api_test.infrastructure.exceptions.NotFoundException;
import com.bim.api_test.interfaces.dtos.response.GeoWheatherResponse;
import com.bim.api_test.interfaces.dtos.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final GeoWheatherService geoWheatherService;

    @GetMapping
    @Operation(summary = "Consulta o clima atual de uma cidade",
            description = "Geocodifica a cidade informada e devolve o clima atual consolidado. A consulta é persistida no histórico.")
    public ResponseEntity<GeoWheatherResponse> getWeather(
            @RequestParam(required = true) String city,
            @RequestParam(required = false) String country
    ) throws BadRequestException, NotFoundException, BadGatewayException, InternalServerErrorException {
        GeoWheatherResponse response = geoWheatherService.searchWheatherAndLocationDetails(city, country);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/history")
    @Operation(summary = "Lista o histórico de consultas",
            description = "Devolve as consultas guardadas, da mais recente para a mais antiga, com filtros opcionais. "
                    + "Paginado: use page (base 0), size e sort (ex.: sort=city,asc).")
    public ResponseEntity<PageResponse<GeoWheatherResponse>> getHistory(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<GeoWheatherResponse> page = geoWheatherService.getHistoricRequests(city, country, startDate, endDate, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(PageResponse.of(page));
    }

    @GetMapping("/history/{id}")
    @Operation(summary = "Consulta um registo do histórico por id")
    public ResponseEntity<GeoWheatherResponse> getHistoryById(
            @Parameter(description = "Identificador do registo") @PathVariable String id
    ) throws NotFoundException {
        GeoWheatherResponse response = geoWheatherService.findByIdResponse(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/history/{id}")
    @Operation(summary = "Elimina um registo do histórico por id")
    public ResponseEntity<Void> deleteHistoryById(
            @Parameter(description = "Identificador do registo") @PathVariable String id
    ) throws NotFoundException {
        geoWheatherService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
