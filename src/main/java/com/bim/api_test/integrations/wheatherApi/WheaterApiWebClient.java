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
import java.util.Locale;

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
            key = "T(com.bim.api_test.integrations.wheatherApi.WheaterApiWebClient).cacheKey(#latitude, #longitude)"
    )
    public WheatherDetailsResponse getCurrentWeather(double latitude, double longitude)
            throws BadGatewayException, InternalServerErrorException {

        String ctx = String.format(Locale.ROOT, "lat=%.4f lon=%.4f", latitude, longitude);
        Instant start = Instant.now();
        logger.info("[weather-api] INÍCIO | {}", ctx);

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
            logger.error("[weather-api] FALHA HTTP | status={} | elapsed={}ms | {} | body={}",
                    ex.getStatusCode(), elapsedMs(start), ctx, shortBody(ex.getResponseBodyAsString()));
            throw new BadGatewayException("Não foi possível consultar o serviço meteorológico.");
        } catch (DecodingException ex) {
            logger.error("[weather-api] FALHA DE PAYLOAD | elapsed={}ms | {}", elapsedMs(start), ctx, ex);
            throw new BadGatewayException("O serviço meteorológico devolveu uma resposta em formato inesperado.");
        } catch (WebClientRequestException ex) {
            logger.error("[weather-api] FALHA DE COMUNICAÇÃO | elapsed={}ms | {} | causa={}",
                    elapsedMs(start), ctx, ex.getMostSpecificCause());
            throw new BadGatewayException("Falha de comunicação ao consultar o serviço meteorológico.");
        } catch (Exception ex) {
            logger.error("[weather-api] FALHA INESPERADA | elapsed={}ms | {}", elapsedMs(start), ctx, ex);
            throw new InternalServerErrorException("Erro inesperado ao consultar a Weather API");
        }

        long elapsed = elapsedMs(start);
        WheatherDetailsResponse body = response != null ? response.getBody() : null;

        if (body == null || body.current() == null || body.currentUnits() == null) {
            logger.error("[weather-api] RESPOSTA INCOMPLETA | elapsed={}ms | {} | body={} current={} units={}",
                    elapsed, ctx, body != null,
                    body != null && body.current() != null,
                    body != null && body.currentUnits() != null);
            throw new BadGatewayException("O serviço meteorológico devolveu uma resposta incompleta.");
        }

        logger.info("[weather-api] OK | elapsed={}ms | {} | timezone={}", elapsed, ctx, body.timezone());

        return body;
    }

    public static String cacheKey(double latitude, double longitude) {
        return String.format(Locale.ROOT, "%.4f|%.4f", latitude, longitude);
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
