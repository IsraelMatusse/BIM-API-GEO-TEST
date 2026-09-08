package com.bim.api_test.integrations.wheatherApi;


import com.bim.api_test.infrastructure.exceptions.BadGatewayException;
import com.bim.api_test.infrastructure.exceptions.InternalServerErrorException;
import com.bim.api_test.integrations.wheatherApi.dtos.WheatherDetailsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

@Component
public class WheaterApiWebClient {

    private static final Logger logger = LoggerFactory.getLogger(WheaterApiWebClient.class);

    private static final String CURRENT_PARAMS =
            "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m";

    private final WebClient client;
    private final WheatherConfig config;

    public WheaterApiWebClient(WebClient client, WheatherConfig config) {
        this.client = client;
        this.config = config;
    }


    @Cacheable(
            cacheNames = "weatherApi",
            key = "T(String).format('%.4f|%.4f', #latitude, #longitude)"
    )
    public WheatherDetailsResponse getCurrentWeather(double latitude, double longitude)
            throws BadGatewayException, InternalServerErrorException {

        Instant start = Instant.now();
        logger.info("Iniciando consulta à Weather Forecast API para latitude={}, longitude={}", latitude, longitude);

        URI uri = UriComponentsBuilder.fromUriString(config.wheatherBaseUrl)
                .path(config.wheatherForecastUrl)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", CURRENT_PARAMS)
                .queryParam("timezone", "auto")
                .build()
                .toUri();

        ResponseEntity<WheatherDetailsResponse> response;
        try {
            response = client.get()
                    .uri(uri)
                    .retrieve()
                    .toEntity(WheatherDetailsResponse.class)
                    .block();
        } catch (WebClientResponseException ex) {
            long elapsed = elapsedMs(start);
            logger.error("Weather API respondeu com erro HTTP {} após {} ms: {}",
                    ex.getStatusCode(), elapsed, ex.getResponseBodyAsString(), ex);
            throw new BadGatewayException("Não foi possível consultar o serviço meteorológico.");
        } catch (DecodingException ex) {
            logger.error("Weather API retornou um payload em formato inesperado após {} ms", elapsedMs(start), ex);
            throw new BadGatewayException("O serviço meteorológico devolveu uma resposta em formato inesperado.");
        } catch (WebClientRequestException ex) {
            logger.error("Falha de comunicação com a Weather API após {} ms (timeout ou conexão recusada)",
                    elapsedMs(start), ex);
            throw new BadGatewayException("Falha de comunicação ao consultar o serviço meteorológico.");
        } catch (Exception ex) {
            logger.error("Erro inesperado ao consultar a Weather API após {} ms", elapsedMs(start), ex);
            throw new InternalServerErrorException("Erro inesperado ao consultar a Weather API");
        }

        long elapsed = elapsedMs(start);
        WheatherDetailsResponse body = response != null ? response.getBody() : null;

        if (body == null || body.current() == null || body.currentUnits() == null) {
            logger.error("Weather API devolveu uma resposta vazia/incompleta após {} ms", elapsed);
            throw new BadGatewayException("O serviço meteorológico devolveu uma resposta incompleta.");
        }

        logger.info("Weather API respondeu com sucesso em {} ms (timezone={})", elapsed, body.timezone());

        return body;
    }

    private long elapsedMs(Instant start) {
        return Duration.between(start, Instant.now()).toMillis();
    }
}
