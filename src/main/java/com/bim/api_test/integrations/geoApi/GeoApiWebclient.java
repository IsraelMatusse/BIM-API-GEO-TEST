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
import java.util.Locale;

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
            key = "T(com.bim.api_test.integrations.geoApi.GeoApiWebclient).cacheKey(#filters)"
    )
    public GeocodingResponse getGeoInfo(GeoCodingFilters filters)
            throws BadRequestException, NotFoundException, BadGatewayException, InternalServerErrorException {

        if (!StringUtils.hasText(filters.name())) {
            logger.warn("[geo-api] PEDIDO REJEITADO | motivo=nome da cidade não informado");
            throw new BadRequestException("O nome da cidade é obrigatório para a busca");
        }

        String city = filters.name().trim();
        String ctx = "cidade='" + city + "' language=" + filters.language() + " count=" + filters.count();
        Instant start = Instant.now();
        logger.info("[geo-api] INÍCIO | {}", ctx);

        URI uri = UriComponentsBuilder.fromUriString(config.geoBaseUrl)
                .path(config.geoSearchUrl)
                .queryParam("name", city)
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
            logger.error("[geo-api] FALHA HTTP | status={} | elapsed={}ms | {} | body={}",
                    ex.getStatusCode(), elapsedMs(start), ctx, shortBody(ex.getResponseBodyAsString()));
            throw new BadGatewayException("Não foi possível consultar o serviço de geocodificação.");
        } catch (DecodingException ex) {
            logger.error("[geo-api] FALHA DE PAYLOAD | elapsed={}ms | {}", elapsedMs(start), ctx, ex);
            throw new BadGatewayException("O serviço de geocodificação devolveu uma resposta em formato inesperado.");
        } catch (WebClientRequestException ex) {
            logger.error("[geo-api] FALHA DE COMUNICAÇÃO | elapsed={}ms | {} | causa={}",
                    elapsedMs(start), ctx, ex.getMostSpecificCause());
            throw new BadGatewayException("Falha de comunicação ao consultar o serviço de geocodificação.");
        } catch (Exception ex) {
            logger.error("[geo-api] FALHA INESPERADA | elapsed={}ms | {}", elapsedMs(start), ctx, ex);
            throw new InternalServerErrorException("Erro inesperado ao consultar a Geo API");
        }

        long elapsed = elapsedMs(start);
        GeocodingResponse body = response != null ? response.getBody() : null;

        if (body == null || body.results() == null) {
            logger.error("[geo-api] RESPOSTA INCOMPLETA | elapsed={}ms | {} | em falta={}",
                    elapsed, ctx, body == null ? "corpo" : "results");
            throw new BadGatewayException("O serviço de geocodificação devolveu uma resposta incompleta.");
        }

        if (body.results().isEmpty()) {
            logger.warn("[geo-api] SEM RESULTADOS | elapsed={}ms | {}", elapsed, ctx);
            throw new NotFoundException("Nenhuma cidade encontrada para '" + city + "'");
        }

        logger.info("[geo-api] OK | elapsed={}ms | {} | resultados={}", elapsed, ctx, body.results().size());

        return body;
    }

    public static String cacheKey(GeoCodingFilters filters) {
        return String.join("|",
                norm(filters.name()), norm(filters.language()),
                String.valueOf(filters.count()), norm(filters.format()));
    }

    private static String norm(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String shortBody(String body) {
        String flat = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        if (flat.isEmpty()) return "<vazio>";
        return flat.length() <= 300 ? flat : flat.substring(0, 300) + "...";
    }

    private long elapsedMs(Instant start) {
        return Duration.between(start, Instant.now()).toMillis();
    }
}
