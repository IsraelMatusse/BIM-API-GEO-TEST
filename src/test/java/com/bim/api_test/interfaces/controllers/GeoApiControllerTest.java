package com.bim.api_test.interfaces.controllers;

import com.bim.api_test.domain.services.GeoService;
import com.bim.api_test.infrastructure.exceptions.BadGatewayException;
import com.bim.api_test.infrastructure.exceptions.NotFoundException;
import com.bim.api_test.infrastructure.logging.CorrelationIdFilter;
import com.bim.api_test.integrations.geoApi.dtos.filters.GeoCodingFilters;
import com.bim.api_test.integrations.geoApi.dtos.response.GeocodingResponse;
import com.bim.api_test.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GeoApiController.class, properties = {
        "api.defaultGeoLanguage=pt",
        "api.defaultGeoResponseFormat=json",
        "api.defaultGeoCount=10"
})
@Import(CorrelationIdFilter.class)
@DisplayName("GET /api/geo-locations")
class GeoApiControllerTest {

    private static final String URL = "/api/geo-locations";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeoService geoService;

    @Test
    @DisplayName("returns 200 with the city payload, keeping the Geo API JSON names")
    void returnsCityPayload() throws Exception {
        when(geoService.getGeoCodeInfo(any()))
                .thenReturn(new GeocodingResponse(List.of(Fixtures.maputo()), 1.23));

        mockMvc.perform(get(URL).param("name", "Maputo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Maputo")))
                .andExpect(jsonPath("$.data.results[0].name").value("Maputo"))
                .andExpect(jsonPath("$.data.results[0].country_code").value("MZ"))
                .andExpect(jsonPath("$.data.generationtime_ms").value(1.23));
    }

    @Test
    @DisplayName("applies the configured defaults for language, format and count")
    void appliesConfiguredDefaults() throws Exception {
        when(geoService.getGeoCodeInfo(any()))
                .thenReturn(new GeocodingResponse(List.of(Fixtures.maputo()), 1.0));

        mockMvc.perform(get(URL).param("name", "Maputo"))
                .andExpect(status().isOk());

        ArgumentCaptor<GeoCodingFilters> captor = ArgumentCaptor.forClass(GeoCodingFilters.class);
        verify(geoService).getGeoCodeInfo(captor.capture());

        GeoCodingFilters filters = captor.getValue();
        assertThat(filters.language()).isEqualTo("pt");
        assertThat(filters.format()).isEqualTo("json");
        assertThat(filters.count()).isEqualTo(10);
    }

    @Test
    @DisplayName("honours the filters explicitly sent in the request")
    void honoursExplicitFilters() throws Exception {
        when(geoService.getGeoCodeInfo(any()))
                .thenReturn(new GeocodingResponse(List.of(Fixtures.lisbon()), 1.0));

        mockMvc.perform(get(URL)
                        .param("name", "Lisboa")
                        .param("geoLanguage", "en")
                        .param("geoCounts", "3"))
                .andExpect(status().isOk());

        ArgumentCaptor<GeoCodingFilters> captor = ArgumentCaptor.forClass(GeoCodingFilters.class);
        verify(geoService).getGeoCodeInfo(captor.capture());

        assertThat(captor.getValue().language()).isEqualTo("en");
        assertThat(captor.getValue().count()).isEqualTo(3);
    }

    @Test
    @DisplayName("returns 400 when the name parameter is missing")
    void returnsBadRequestWhenNameIsMissing() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("name")));
    }

    @Test
    @DisplayName("returns 404 when the Geo API finds no city")
    void returnsNotFoundWhenNoCityMatches() throws Exception {
        when(geoService.getGeoCodeInfo(any()))
                .thenThrow(new NotFoundException("Nenhuma cidade encontrada para 'Xpto'"));

        mockMvc.perform(get(URL).param("name", "Xpto"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Xpto")));
    }

    @Test
    @DisplayName("returns 502 with a correlationId when the upstream service fails")
    void returnsBadGatewayWhenUpstreamFails() throws Exception {
        when(geoService.getGeoCodeInfo(any()))
                .thenThrow(new BadGatewayException("upstream down"));

        mockMvc.perform(get(URL).param("name", "Maputo"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    @DisplayName("reuses the X-Correlation-Id sent by the client")
    void reusesClientCorrelationId() throws Exception {
        when(geoService.getGeoCodeInfo(any()))
                .thenReturn(new GeocodingResponse(List.of(Fixtures.maputo()), 1.0));

        mockMvc.perform(get(URL)
                        .param("name", "Maputo")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-123"));
    }
}
