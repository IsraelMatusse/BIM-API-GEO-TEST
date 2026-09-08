package com.bim.api_test.domain.services;

import com.bim.api_test.domain.entities.GeoWhetherHistory;
import com.bim.api_test.domain.repositories.GeoWheatherRepo;
import com.bim.api_test.infrastructure.exceptions.BadRequestException;
import com.bim.api_test.infrastructure.exceptions.InternalServerErrorException;
import com.bim.api_test.infrastructure.exceptions.NotFoundException;
import com.bim.api_test.integrations.geoApi.GeoApiWebclient;
import com.bim.api_test.integrations.geoApi.dtos.filters.GeoCodingFilters;
import com.bim.api_test.integrations.geoApi.dtos.response.GeocodingResponse;
import com.bim.api_test.integrations.wheatherApi.WheaterApiWebClient;
import com.bim.api_test.interfaces.dtos.response.GeoWheatherResponse;
import com.bim.api_test.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeoWheatherService")
class GeoWheatherServiceTest {

    @Mock
    private GeoWheatherRepo repo;

    @Mock
    private GeoApiWebclient geoApiWebclient;

    @Mock
    private WheaterApiWebClient wheaterApiWebClient;

    @InjectMocks
    private GeoWheatherService service;

    @BeforeEach
    void injectConfiguredDefaults() {
        service.defaultGeoLanguage = "pt";
        service.defaultGeoFormat = "json";
        service.defaultGeoCount = 10;
    }

    private void repoSaveReturnsEntityWithId(String id) {
        when(repo.save(any(GeoWhetherHistory.class))).thenAnswer(invocation -> {
            GeoWhetherHistory entity = invocation.getArgument(0);
            entity.setId(id);
            return entity;
        });
    }

    private void geoAndWeatherRespond() throws Exception {
        when(geoApiWebclient.getGeoInfo(any()))
                .thenReturn(new GeocodingResponse(List.of(Fixtures.maputo()), 1.0));
        when(wheaterApiWebClient.getCurrentWeather(anyDouble(), anyDouble())).thenReturn(Fixtures.weather());
    }

    @Test
    @DisplayName("rejects a blank city without calling the upstream APIs")
    void rejectsBlankCity() {
        assertThatThrownBy(() -> service.searchWheatherAndLocationDetails("   ", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("city");

        verifyNoInteractions(geoApiWebclient, wheaterApiWebClient, repo);
    }

    @Test
    @DisplayName("trims the city and queries the Geo API with the configured defaults")
    void queriesGeoApiWithConfiguredDefaults() throws Exception {
        geoAndWeatherRespond();
        repoSaveReturnsEntityWithId("hist-1");

        service.searchWheatherAndLocationDetails("  Maputo  ", null);

        ArgumentCaptor<GeoCodingFilters> captor = ArgumentCaptor.forClass(GeoCodingFilters.class);
        verify(geoApiWebclient).getGeoInfo(captor.capture());

        GeoCodingFilters filters = captor.getValue();
        assertThat(filters.name()).isEqualTo("Maputo");
        assertThat(filters.language()).isEqualTo("pt");
        assertThat(filters.format()).isEqualTo("json");
        assertThat(filters.count()).isEqualTo(10);
    }

    @Test
    @DisplayName("calls the Weather API with the coordinates of the selected location")
    void usesTheCoordinatesOfTheSelectedLocation() throws Exception {
        geoAndWeatherRespond();
        repoSaveReturnsEntityWithId("hist-1");

        GeoWheatherResponse response = service.searchWheatherAndLocationDetails("Maputo", null);

        verify(wheaterApiWebClient).getCurrentWeather(Fixtures.MAPUTO_LATITUDE, Fixtures.MAPUTO_LONGITUDE);
        assertThat(response.id()).isEqualTo("hist-1");
        assertThat(response.city()).isEqualTo("Maputo");
        assertThat(response.temperature()).isEqualTo(27.4);
        assertThat(response.temperatureUnit()).isEqualTo("C");
        assertThat(response.humidity()).isEqualTo(68);
        assertThat(response.consultedAt()).isNotNull();
    }

    @Test
    @DisplayName("picks the candidate matching the country, not simply the first result")
    void filtersCandidatesByCountry() throws Exception {
        when(geoApiWebclient.getGeoInfo(any()))
                .thenReturn(new GeocodingResponse(List.of(Fixtures.lisbon(), Fixtures.maputo()), 1.0));
        when(wheaterApiWebClient.getCurrentWeather(anyDouble(), anyDouble())).thenReturn(Fixtures.weather());
        repoSaveReturnsEntityWithId("hist-1");

        service.searchWheatherAndLocationDetails("Cidade", "MZ");

        verify(wheaterApiWebClient).getCurrentWeather(Fixtures.MAPUTO_LATITUDE, Fixtures.MAPUTO_LONGITUDE);
    }

    @Test
    @DisplayName("throws NotFound when no candidate matches the country")
    void throwsNotFoundWhenNoCandidateMatchesCountry() throws Exception {
        when(geoApiWebclient.getGeoInfo(any()))
                .thenReturn(new GeocodingResponse(List.of(Fixtures.lisbon()), 1.0));

        assertThatThrownBy(() -> service.searchWheatherAndLocationDetails("Lisboa", "MZ"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("MZ");

        verifyNoInteractions(wheaterApiWebClient);
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("turns a persistence failure into InternalServerError")
    void turnsPersistenceFailureIntoInternalServerError() throws Exception {
        geoAndWeatherRespond();
        when(repo.save(any(GeoWhetherHistory.class))).thenThrow(new RuntimeException("connection lost"));

        assertThatThrownBy(() -> service.searchWheatherAndLocationDetails("Maputo", null))
                .isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    @DisplayName("history defaults to newest first when the request carries no sort")
    void historyDefaultsToNewestFirst() {
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(Fixtures.savedHistory("hist-1")), PageRequest.of(0, 20), 1));

        service.getHistoricRequests(null, null, null, null, PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "consultedAt"));
    }

    @Test
    @DisplayName("history keeps an explicit sort from the caller")
    void historyKeepsExplicitSort() {
        Pageable requested = PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "city"));
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), requested, 0));

        service.getHistoricRequests(null, null, null, null, requested);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findAll(any(Specification.class), captor.capture());

        Pageable used = captor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(1);
        assertThat(used.getPageSize()).isEqualTo(5);
        assertThat(used.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "city"));
    }

    @Test
    @DisplayName("history maps entities to responses and preserves the page metadata")
    void historyMapsEntitiesAndPreservesMetadata() {
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(Fixtures.savedHistory("hist-1"), Fixtures.savedHistory("hist-2")),
                        PageRequest.of(0, 2), 7));

        Page<GeoWheatherResponse> page = service.getHistoricRequests("Maputo", null, null, null, PageRequest.of(0, 2));

        assertThat(page.getContent()).extracting(GeoWheatherResponse::id).containsExactly("hist-1", "hist-2");
        assertThat(page.getTotalElements()).isEqualTo(7);
        assertThat(page.getTotalPages()).isEqualTo(4);
        assertThat(page.getContent().get(0).consultedAt()).isEqualTo(Fixtures.CONSULTED_AT_FORMATTED);
    }

    @Test
    @DisplayName("findById ignores soft-deleted records")
    void findByIdIgnoresSoftDeletedRecords() {
        when(repo.findByIdAndDeletedAtIsNull("does-not-exist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("does-not-exist"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("delete is a soft delete: sets deletedAt and saves again")
    void deleteSetsDeletedAtAndSaves() throws Exception {
        GeoWhetherHistory existing = Fixtures.savedHistory("hist-1");
        when(repo.findByIdAndDeletedAtIsNull("hist-1")).thenReturn(Optional.of(existing));

        service.delete("hist-1");

        ArgumentCaptor<GeoWhetherHistory> captor = ArgumentCaptor.forClass(GeoWhetherHistory.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        assertThat(captor.getValue().getId()).isEqualTo("hist-1");
    }
}
