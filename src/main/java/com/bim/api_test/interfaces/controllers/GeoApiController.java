package com.bim.api_test.interfaces.controllers;

import com.bim.api_test.domain.services.GeoService;
import com.bim.api_test.infrastructure.exceptions.BadGatewayException;
import com.bim.api_test.infrastructure.exceptions.BadRequestException;
import com.bim.api_test.infrastructure.exceptions.InternalServerErrorException;
import com.bim.api_test.infrastructure.exceptions.NotFoundException;
import com.bim.api_test.integrations.geoApi.dtos.filters.GeoCodingFilters;
import com.bim.api_test.integrations.geoApi.dtos.response.GeocodingResponse;
import com.bim.api_test.interfaces.dtos.response.ResponseApi;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/geo-locations")
@RequiredArgsConstructor
public class GeoApiController {

    private final GeoService geoService;

    @GetMapping
    @Operation(summary = "Busca dados de geolocalização de uma cidade", description = "Busca dados de geolocalização de uma cidade")
    public ResponseEntity<ResponseApi> getGeoInfo (            @RequestParam(required = true) String name,
                                                               @RequestParam(required = false, defaultValue = "${api.defaultGeoLanguage}") String geoLanguage,
                                                               @RequestParam(required = false, defaultValue = "${api.defaultGeoResponseFormat}") String geoFormat,
                                                               @RequestParam(required = false, defaultValue = "${api.defaultGeoCount}") Integer geoCounts) throws BadGatewayException, BadRequestException, NotFoundException, InternalServerErrorException {
        GeoCodingFilters geoCodingFilters= new GeoCodingFilters(name, geoLanguage, geoCounts, geoFormat);
        GeocodingResponse result= geoService.getGeoCodeInfo(geoCodingFilters);
        return ResponseEntity.status(HttpStatus.OK).body(new ResponseApi("Informação da cidade selecionada " + geoCodingFilters.name() + " Retornada com sucesso", result));
    }
}
