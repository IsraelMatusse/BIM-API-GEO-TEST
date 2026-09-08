package com.bim.api_test.domain.services;

import com.bim.api_test.infrastructure.exceptions.BadGatewayException;
import com.bim.api_test.infrastructure.exceptions.BadRequestException;
import com.bim.api_test.infrastructure.exceptions.InternalServerErrorException;
import com.bim.api_test.infrastructure.exceptions.NotFoundException;
import com.bim.api_test.integrations.geoApi.GeoApiWebclient;
import com.bim.api_test.integrations.geoApi.dtos.filters.GeoCodingFilters;
import com.bim.api_test.integrations.geoApi.dtos.response.GeocodingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeoService {

    private final GeoApiWebclient geoApiWebclient;


    public GeocodingResponse getGeoCodeInfo (GeoCodingFilters filters) throws BadGatewayException, BadRequestException, NotFoundException, InternalServerErrorException {
        return geoApiWebclient.getGeoApiInfo(filters);
    }
}
