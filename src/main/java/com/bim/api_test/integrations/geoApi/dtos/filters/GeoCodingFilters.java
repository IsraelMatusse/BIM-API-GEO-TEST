package com.bim.api_test.integrations.geoApi.dtos.filters;

public record GeoCodingFilters(
        String name,
        String language,
        int count,
        String format
) {
}
