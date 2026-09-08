package com.bim.api_test.interfaces.controllers;

import com.bim.api_test.domain.services.GeoWheatherService;
import com.bim.api_test.infrastructure.exceptions.BadRequestException;
import com.bim.api_test.infrastructure.exceptions.NotFoundException;
import com.bim.api_test.infrastructure.logging.CorrelationIdFilter;
import com.bim.api_test.interfaces.dtos.response.GeoWheatherResponse;
import com.bim.api_test.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WeatherController.class)
@Import(CorrelationIdFilter.class)
@DisplayName("/api/weather")
class WeatherControllerTest {

    private static final String URL = "/api/weather";
    private static final String HISTORY_URL = "/api/weather/history";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeoWheatherService geoWheatherService;

    private static GeoWheatherResponse response(String id) {
        return new GeoWheatherResponse(Fixtures.savedHistory(id));
    }

    @Test
    @DisplayName("GET returns 200 with the consolidated weather")
    void returnsConsolidatedWeather() throws Exception {
        when(geoWheatherService.searchWheatherAndLocationDetails(any(), any()))
                .thenReturn(response("hist-1"));

        mockMvc.perform(get(URL).param("city", "Maputo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("hist-1"))
                .andExpect(jsonPath("$.city").value("Maputo"))
                .andExpect(jsonPath("$.countryCode").value("MZ"))
                .andExpect(jsonPath("$.temperature").value(27.4))
                .andExpect(jsonPath("$.temperatureUnit").value("C"))
                .andExpect(jsonPath("$.humidity").value(68))
                .andExpect(jsonPath("$.windSpeed").value(11.2));
    }

    @Test
    @DisplayName("GET renders consultedAt as dd-MM-yyyy HH:mm in the record time zone")
    void rendersConsultedAtInTheRecordTimeZone() throws Exception {
        when(geoWheatherService.searchWheatherAndLocationDetails(any(), any()))
                .thenReturn(response("hist-1"));

        mockMvc.perform(get(URL).param("city", "Maputo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultedAt").value(Fixtures.CONSULTED_AT_FORMATTED));
    }

    @Test
    @DisplayName("GET passes city and a null country straight to the service")
    void passesCityAndCountryToTheService() throws Exception {
        when(geoWheatherService.searchWheatherAndLocationDetails(any(), any()))
                .thenReturn(response("hist-1"));

        mockMvc.perform(get(URL).param("city", "Maputo"))
                .andExpect(status().isOk());

        verify(geoWheatherService).searchWheatherAndLocationDetails(eq("Maputo"), isNull());
    }

    @Test
    @DisplayName("GET returns 400 when the city parameter is missing")
    void returnsBadRequestWhenCityIsMissing() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("city")));
    }

    @Test
    @DisplayName("GET returns 400 when the service rejects the city")
    void returnsBadRequestWhenServiceRejectsCity() throws Exception {
        when(geoWheatherService.searchWheatherAndLocationDetails(any(), any()))
                .thenThrow(new BadRequestException("O parametro 'city' e obrigatorio e nao pode ser vazio"));

        mockMvc.perform(get(URL).param("city", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("city")));
    }

    @Test
    @DisplayName("GET returns 404 when the city does not exist in the given country")
    void returnsNotFoundForUnknownCityInCountry() throws Exception {
        when(geoWheatherService.searchWheatherAndLocationDetails(any(), any()))
                .thenThrow(new NotFoundException("Nenhuma cidade encontrada para 'Maputo' no pais 'PT'"));

        mockMvc.perform(get(URL).param("city", "Maputo").param("country", "PT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /history returns the paginated envelope")
    void returnsPaginatedHistory() throws Exception {
        Page<GeoWheatherResponse> page = new PageImpl<>(
                List.of(response("hist-1"), response("hist-2")), PageRequest.of(0, 20), 42);
        when(geoWheatherService.getHistoricRequests(any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(HISTORY_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value("hist-1"))
                .andExpect(jsonPath("$.content[0].consultedAt").value(Fixtures.CONSULTED_AT_FORMATTED))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(42))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    @DisplayName("GET /history defaults to page 0 with size 20")
    void appliesPaginationDefaults() throws Exception {
        when(geoWheatherService.getHistoricRequests(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get(HISTORY_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(geoWheatherService).getHistoricRequests(any(), any(), any(), any(), captor.capture());

        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("GET /history honours page, size and sort from the query string")
    void honoursPaginationParameters() throws Exception {
        when(geoWheatherService.getHistoricRequests(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get(HISTORY_URL)
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "city,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(geoWheatherService).getHistoricRequests(any(), any(), any(), any(), captor.capture());

        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "city"));
    }

    @Test
    @DisplayName("GET /history binds the ISO date filters to LocalDate")
    void bindsIsoDateFilters() throws Exception {
        when(geoWheatherService.getHistoricRequests(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get(HISTORY_URL)
                        .param("city", "Maputo")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        verify(geoWheatherService).getHistoricRequests(
                eq("Maputo"), isNull(), eq(LocalDate.of(2026, 9, 1)), eq(LocalDate.of(2026, 9, 8)), any());
    }

    @Test
    @DisplayName("GET /history/{id} returns the record")
    void returnsHistoryRecordById() throws Exception {
        when(geoWheatherService.findByIdResponse("hist-1")).thenReturn(response("hist-1"));

        mockMvc.perform(get(HISTORY_URL + "/hist-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("hist-1"));
    }

    @Test
    @DisplayName("GET /history/{id} returns 404 for an unknown id")
    void returnsNotFoundForUnknownHistoryId() throws Exception {
        when(geoWheatherService.findByIdResponse("does-not-exist"))
                .thenThrow(new NotFoundException("Historico nao encontrado"));

        mockMvc.perform(get(HISTORY_URL + "/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Historico")));
    }

    @Test
    @DisplayName("DELETE /history/{id} returns 204 with no body")
    void deletesHistoryRecord() throws Exception {
        mockMvc.perform(delete(HISTORY_URL + "/hist-1"))
                .andExpect(status().isNoContent());

        verify(geoWheatherService).delete("hist-1");
    }

    @Test
    @DisplayName("DELETE /history/{id} returns 404 for an unknown id")
    void deleteReturnsNotFoundForUnknownId() throws Exception {
        doThrow(new NotFoundException("Historico nao encontrado"))
                .when(geoWheatherService).delete("does-not-exist");

        mockMvc.perform(delete(HISTORY_URL + "/does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
