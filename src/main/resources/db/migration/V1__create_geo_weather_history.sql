
CREATE TABLE geo_weather_history (
    id                      VARCHAR(36)                 NOT NULL,

    city                    VARCHAR(150)                NOT NULL,
    country                 VARCHAR(150),
    country_code            VARCHAR(8),
    region                  VARCHAR(150),
    latitude                DOUBLE PRECISION            NOT NULL,
    longitude               DOUBLE PRECISION            NOT NULL,
    time_zone               VARCHAR(64),

    temperature             DOUBLE PRECISION,
    temperature_unit        VARCHAR(16),
    apparent_temperature    DOUBLE PRECISION,
    humidity                INTEGER,
    humidity_unit           VARCHAR(16),
    wind_speed              DOUBLE PRECISION,
    wind_speed_unit         VARCHAR(16),
    weather_code            INTEGER,
    weather_time            VARCHAR(32),

    consulted_at            TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    created_at              TIMESTAMP(6),
    updated_at              TIMESTAMP(6),
    deleted_at              TIMESTAMP(6),

    CONSTRAINT pk_geo_weather_history           PRIMARY KEY (id),
    CONSTRAINT ck_geo_weather_history_latitude  CHECK (latitude  BETWEEN  -90 AND  90),
    CONSTRAINT ck_geo_weather_history_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_geo_weather_history_humidity  CHECK (humidity IS NULL OR humidity BETWEEN 0 AND 100)
);

CREATE INDEX ix_geo_weather_history_active_consulted_at
    ON geo_weather_history (consulted_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_geo_weather_history_coords
    ON geo_weather_history (latitude, longitude, consulted_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_geo_weather_history_city
    ON geo_weather_history (LOWER(city))
    WHERE deleted_at IS NULL;

COMMENT ON TABLE  geo_weather_history               IS 'Histórico de consultas combinadas Geo API + Weather Forecast API';
COMMENT ON COLUMN geo_weather_history.consulted_at  IS 'Momento em que a nossa API fez a consulta (UTC)';
COMMENT ON COLUMN geo_weather_history.weather_time  IS 'Instante da medição';
COMMENT ON COLUMN geo_weather_history.deleted_at    IS 'Soft delete: NULL = registo activo';
