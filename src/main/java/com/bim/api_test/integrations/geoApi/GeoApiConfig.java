package com.bim.api_test.integrations.geoApi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration

public class GeoApiConfig {

    @Value("${geo.api.base.url}")
    public String geoBaseUrl;
    @Value("${geo.api.search}")
    public String geoSearchUrl;
}
