package com.bim.api_test.domain.services;

import com.bim.api_test.domain.entities.GeoWhetherHistory;
import com.bim.api_test.domain.repositories.GeoWheatherRepo;
import com.bim.api_test.infrastructure.exceptions.BadGatewayException;
import com.bim.api_test.infrastructure.exceptions.BadRequestException;
import com.bim.api_test.infrastructure.exceptions.InternalServerErrorException;
import com.bim.api_test.infrastructure.exceptions.NotFoundException;
import com.bim.api_test.integrations.geoApi.GeoApiWebclient;
import com.bim.api_test.integrations.geoApi.dtos.filters.GeoCodingFilters;
import com.bim.api_test.integrations.geoApi.dtos.response.GeocodingResponse;
import com.bim.api_test.integrations.geoApi.dtos.response.GeocodingResultDto;
import com.bim.api_test.integrations.wheatherApi.WheaterApiWebClient;
import com.bim.api_test.integrations.wheatherApi.dtos.WheatherDetailsResponse;
import com.bim.api_test.interfaces.dtos.response.GeoWheatherResponse;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GeoWheatherService {

    private static final Logger logger = LoggerFactory.getLogger(GeoWheatherService.class);


    @org.springframework.beans.factory.annotation.Value("${api.defaultGeoLanguage}")
    public String defaultGeoLanguage;
    @org.springframework.beans.factory.annotation.Value("${api.defaultGeoResponseFormat}")
    public String defaultGeoFormat;
    @org.springframework.beans.factory.annotation.Value("${api.defaultGeoCount}")
    public Integer defaultGeoCount;
    private final GeoWheatherRepo geoWheatherRepo;
    private final GeoApiWebclient geoApiWebclient;
    private final WheaterApiWebClient wheaterApiWebClient;


    public GeoWheatherResponse searchWheatherAndLocationDetails(String city, String country)
            throws BadRequestException, NotFoundException, BadGatewayException, InternalServerErrorException {

        if (!StringUtils.hasText(city)) {
            logger.warn("Consulta de clima rejeitada: cidade ausente ou vazia");
            throw new BadRequestException("O parâmetro 'city' é obrigatório e não pode ser vazio");
        }

        String cityTrimmed = city.trim();
        String countryTrimmed = StringUtils.hasText(country) ? country.trim() : null;
        Instant start = Instant.now();

        logger.info("Início da consulta de clima: city='{}', country='{}'", cityTrimmed, countryTrimmed);

        GeocodingResponse geoResponse = geoApiWebclient.getGeoApiInfo(
                new GeoCodingFilters(cityTrimmed, defaultGeoLanguage, defaultGeoCount, defaultGeoFormat));

        GeocodingResultDto location = selectLocation(geoResponse.results(), cityTrimmed, countryTrimmed);

        WheatherDetailsResponse weather = wheaterApiWebClient.getCurrentWeather(location.latitude(), location.longitude());

        GeoWhetherHistory entity = new GeoWhetherHistory(location, weather);

        GeoWhetherHistory saved;
        try {
            saved = geoWheatherRepo.save(entity);
            logger.info("Consulta de clima persistida com sucesso: id={}", saved.getId());
        } catch (Exception ex) {
            logger.error("Falha ao persistir a consulta de clima para '{}'", cityTrimmed, ex);
            throw new InternalServerErrorException("Não foi possível guardar o histórico da consulta.");
        }

        long elapsed = java.time.Duration.between(start, Instant.now()).toMillis();
        logger.info("Fim da consulta de clima para '{}' em {} ms (id={})", cityTrimmed, elapsed, saved.getId());

        return new GeoWheatherResponse(saved);
    }


    private GeocodingResultDto selectLocation(List<GeocodingResultDto> results, String city, String country) throws NotFoundException {
        List<GeocodingResultDto> candidates = results;

        if (country != null) {
            String countryLower = country.toLowerCase(Locale.ROOT);
            List<GeocodingResultDto> filtered = results.stream()
                    .filter(r -> matches(r.country(), countryLower) || matches(r.countryCode(), countryLower))
                    .toList();

            if (filtered.isEmpty()) {
                logger.warn("Nenhum resultado para '{}' corresponde ao país '{}'", city, country);
                throw new NotFoundException("Nenhuma cidade encontrada para '" + city + "' no país '" + country + "'");
            }
            candidates = filtered;
        }

        GeocodingResultDto selected = candidates.get(0);
        logger.info("Localização selecionada para '{}': id={}, country={}, admin1={} (de {} candidato(s))",
                city, selected.id(), selected.country(), selected.admin1(), candidates.size());

        return selected;
    }

    private boolean matches(String value, String targetLower) {
        return value != null && value.toLowerCase(Locale.ROOT).equals(targetLower);
    }



    private static final Sort DEFAULT_HISTORY_SORT = Sort.by(Sort.Direction.DESC, "consultedAt");

    public Page<GeoWheatherResponse> getHistoricRequests(String city, String country,
                                                         LocalDate startDate, LocalDate endDate,
                                                         Pageable pageable) {
        Specification<GeoWhetherHistory> spec = buildHistorySpecification(city, country, startDate, endDate);
        Pageable effective = withDefaultSort(pageable);

        Page<GeoWhetherHistory> results = geoWheatherRepo.findAll(spec, effective);

        logger.info("Histórico consultado (city='{}', country='{}', startDate={}, endDate={}, page={}, size={}): "
                        + "{} de {} registo(s)",
                city, country, startDate, endDate,
                effective.getPageNumber(), effective.getPageSize(),
                results.getNumberOfElements(), results.getTotalElements());

        return results.map(GeoWheatherResponse::new);
    }

    private Pageable withDefaultSort(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, DEFAULT_HISTORY_SORT);
        }
        return pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_HISTORY_SORT);
    }

    private Specification<GeoWhetherHistory> buildHistorySpecification(String city, String country, LocalDate startDate, LocalDate endDate) {
        Specification<GeoWhetherHistory> spec = (root, query, cb) -> cb.conjunction();

        spec=spec.and((root, query, cb) -> cb.isNull(root.get("deletedAt")));
        if (StringUtils.hasText(city)) {
            String cityLower = city.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("city")), cityLower));
        }
        if (StringUtils.hasText(country)) {
            String countryLower = country.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("country")), countryLower));
        }
        if (startDate != null) {
            Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("consultedAt"), startInstant));
        }
        if (endDate != null) {
            Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("consultedAt"), endInstant));
        }
        return spec;
    }


    public void delete(String id) throws NotFoundException {
        GeoWhetherHistory geoWhetherHistory = findById(id);
        geoWhetherHistory.delete();
        geoWheatherRepo.save(geoWhetherHistory);

        logger.info("Registo de histórico eliminado: id={}", id);
    }

    public GeoWhetherHistory findById(String id) throws NotFoundException {

        return geoWheatherRepo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(()-> new NotFoundException("Historico não encontrado"));
    }

    public GeoWheatherResponse findByIdResponse(String id) throws NotFoundException {
        GeoWhetherHistory historic = this.findById(id);
        return new GeoWheatherResponse(historic);
    }
}
