package com.bim.api_test.integrations.geoApi;

import com.bim.api_test.infrastructure.exceptions.BadGatewayException;
import com.bim.api_test.infrastructure.exceptions.BadRequestException;
import com.bim.api_test.infrastructure.exceptions.InternalServerErrorException;
import com.bim.api_test.infrastructure.exceptions.NotFoundException;
import com.bim.api_test.integrations.geoApi.dtos.filters.GeoCodingFilters;
import com.bim.api_test.integrations.geoApi.dtos.response.GeocodingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

@Component
public class GeoApiWebclient {

    private static final Logger logger = LoggerFactory.getLogger(GeoApiWebclient.class);

    private final WebClient client;
    private final GeoApiConfig config;

    public GeoApiWebclient(WebClient client, GeoApiConfig config) {
        this.config = config;
        this.client = client;
    }


    @Cacheable(
            cacheNames = "geoApi",
            key = "#filters.name().trim().toLowerCase() + '|' + #filters.language() + '|' + #filters.count() + '|' + #filters.format()"
    )
    public GeocodingResponse getGeoInfo(GeoCodingFilters filters)
            throws BadRequestException, NotFoundException, BadGatewayException, InternalServerErrorException {

        if (!StringUtils.hasText(filters.name())) {
            logger.error("Busca na Geo API rejeitada: parametro cidade não informado");
            throw new BadRequestException("O nome da cidade é obrigatório para a busca");
        }

        String city = filters.name().trim();
        Instant start = Instant.now();
        logger.info("Iniciando consulta à Geo API com os parametros: '{}' (language={}, count={})",
                city, filters.language(), filters.count());

        URI uri = UriComponentsBuilder.fromUriString(config.geoBaseUrl)
                .path(config.geoSearchUrl)
                .queryParam("name", filters.name())
                .queryParam("count", filters.count())
                .queryParam("language", filters.language())
                .queryParam("format", filters.format())
                .build()
                .toUri();

        ResponseEntity<GeocodingResponse> response;
        try {
            response = client.get()
                    .uri(uri)
                    .retrieve()
                    .toEntity(GeocodingResponse.class)
                    .block();
        } catch (WebClientResponseException ex) {
            long elapsed = elapsedMs(start);
            logger.error("Geo API respondeu com erro HTTP {} para '{}' após {} ms: {}",
                    ex.getStatusCode(), city, elapsed, ex.getResponseBodyAsString(), ex);
            throw new BadGatewayException("Não foi possível consultar o serviço de geocodificação.");
        } catch (DecodingException ex) {
            logger.error("Geo API retornou um payload em formato inesperado para '{}' após {} ms",
                    city, elapsedMs(start), ex);
            throw new BadGatewayException("O serviço de geocodificação devolveu uma resposta em formato inesperado.");
        } catch (WebClientRequestException ex) {
            logger.error("Falha de comunicação com a Geo API para '{}' após {} ms ",
                    city, elapsedMs(start), ex);
            throw new BadGatewayException("Falha de comunicação ao consultar o serviço de geocodificação.");
        } catch (Exception ex) {
            logger.error("Erro inesperado ao consultar a Geo API para '{}' após {} ms",
                    city, elapsedMs(start), ex);
            throw new InternalServerErrorException("Erro inesperado ao consultar a Geo API");
        }

        //terminou a consulta
        long elapsed = elapsedMs(start);
        GeocodingResponse body = response != null ? response.getBody() : null;

        if (body == null || body.results() == null) {
            logger.error("Geo API devolveu uma resposta vazia ou incompleta para '{}' após {} ms", city, elapsed);
            throw new BadGatewayException("O serviço de geocodificação devolveu uma resposta incompleta.");
        }

        if (body.results().isEmpty()) {
            logger.warn("Geo API não retornou resultados para '{}' (concluído em {} ms)", city, elapsed);
            throw new NotFoundException("Nenhuma cidade encontrada para '" + city + "'");
        }

        logger.info("Geo API retornou {} resultado(s) para '{}' em {} ms",
                body.results().size(), city, elapsed);

        return body;
    }

    private long elapsedMs(Instant start) {
        return Duration.between(start, Instant.now()).toMillis();
    }
}
