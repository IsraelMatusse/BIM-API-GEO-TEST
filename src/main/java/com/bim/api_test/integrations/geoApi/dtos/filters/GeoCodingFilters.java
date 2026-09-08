package com.bim.api_test.integrations.geoApi.dtos.filters;

import io.swagger.v3.oas.annotations.Parameter;

public record GeoCodingFilters(
        @Parameter(required = true)
        String name,
        @Parameter(required = false)
        String language,
        @Parameter(required = false)
        int count,
        @Parameter(required = false)
        String format
) {
}
