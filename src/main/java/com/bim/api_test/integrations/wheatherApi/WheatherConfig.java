package com.bim.api_test.integrations.wheatherApi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WheatherConfig {

    @Value("${wheather.api.base.url}")
    public String wheatherBaseUrl;

    @Value("${wheather.api.forecast}")
    public String wheatherForecastUrl;
}
