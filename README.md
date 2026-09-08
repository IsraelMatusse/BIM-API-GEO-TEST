# BIM API — Geolocalização e Meteorologia

API REST que combina duas APIs públicas da Open-Meteo — geocodificação de cidades e previsão
meteorológica — numa única consulta, guarda cada consulta em histórico e devolve-a paginada.

Construída para o teste prático do Millennium BIM.

---

## Stack

| Componente | Escolha |
|---|---|
| Runtime | Java 17, Spring Boot 4.0.8 |
| Web | Spring MVC (endpoints) + WebClient (chamadas externas) |
| Persistência | PostgreSQL, Spring Data JPA, Flyway |
| Cache | Caffeine |
| Documentação | springdoc-openapi 3.1.1 (Swagger UI) |
| Testes | JUnit 5, Mockito, MockMvc, AssertJ |

## Arquitectura

Camadas separadas por responsabilidade, sem dependências a apontar para dentro:

```
interfaces/      controllers, DTOs de resposta   <- fronteira HTTP
domain/          services, entities, repositories <- regras de negócio
integrations/    web clients das APIs externas    <- fronteira de saída
infrastructure/  config, exceptions, logging      <- transversal
```

Um pedido a `/api/weather` atravessa: `WeatherController` → `GeoWheatherService` →
`GeoApiWebclient` (geocodifica) → `WheaterApiWebClient` (clima) → `GeoWheatherRepo` (persiste)
→ `GeoWheatherResponse`.

---

## Como correr

### Pré-requisitos

- JDK 17+
- PostgreSQL 14+ a correr localmente

### 1. Base de dados

```bash
createdb bim_api_test_geo_db
```

O Flyway cria o esquema no arranque. Não é preciso correr SQL à mão.

### 2. Variáveis de ambiente

```bash
cp .env.example .env      # preencher DATABASE_PASS
set -a && source .env && set +a
```


### 3. Arrancar

```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8282` (`SERVER_PORT`).

- Swagger UI — <http://localhost:8282/swagger-ui/index.html>
- OpenAPI JSON — <http://localhost:8282/v3/api-docs>
- Actuator — <http://localhost:8282/actuator/health>

---

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/health` | Estado e versão da aplicação |
| `GET` | `/api/geo-locations` | Geocodificação: procura uma cidade |
| `GET` | `/api/weather` | Clima actual de uma cidade (persiste no histórico) |
| `GET` | `/api/weather/history` | Histórico paginado, com filtros |
| `GET` | `/api/weather/history/{id}` | Um registo do histórico |
| `DELETE` | `/api/weather/history/{id}` | Elimina um registo (soft delete) |

### Clima actual

```bash
curl "http://localhost:8282/api/weather?city=Maputo&country=MZ"
```

`country` é opcional e desempata quando existem cidades com o mesmo nome em países diferentes.

```json
{
  "id": "0198a5e2-1c2f-7a11-9f3b-2b7d5c8e9a01",
  "city": "Maputo",
  "country": "Mozambique",
  "countryCode": "MZ",
  "region": "Maputo City",
  "latitude": -25.96553,
  "longitude": 32.58322,
  "timezone": "Africa/Maputo",
  "temperature": 27.4,
  "temperatureUnit": "°C",
  "apparentTemperature": 29.1,
  "humidity": 68,
  "humidityUnit": "%",
  "windSpeed": 11.2,
  "windSpeedUnit": "km/h",
  "weatherCode": 3,
  "weatherTime": "2026-09-08T09:00",
  "consultedAt": "08-09-2026 09:00"
}
```

### Histórico paginado

```bash
curl "http://localhost:8282/api/weather/history?city=Maputo&startDate=2026-09-01&page=0&size=20"
```

| Parâmetro | Omissão | Notas |
|---|---|---|
| `city`, `country` | — | Comparação sem distinguir maiúsculas |
| `startDate`, `endDate` | — | ISO `yyyy-MM-dd`, intervalo inclusivo |
| `page` | `0` | Base 0 |
| `size` | `20` | Máximo 100 |
| `sort` | `consultedAt,desc` | Ex.: `sort=city,asc` |

```json
{
  "content": [ { "id": "...", "city": "Maputo", "...": "..." } ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

O envelope é um record próprio (`PageResponse`) em vez do `Page` do Spring Data, cuja estrutura
JSON não é um contrato estável entre versões.

### Geocodificação

```bash
curl "http://localhost:8282/api/geo-locations?name=Maputo&geoLanguage=pt&geoCounts=5"
```

### Erros

Todas as falhas devolvem o mesmo formato:

```json
{ "message": "Nenhuma cidade encontrada para 'Xpto'", "correlationId": "49ef2a4c-298f-4dee-8abd-21b8721e5f79" }
```

| Código | Quando |
|---|---|
| `400` | Parâmetro obrigatório em falta ou inválido |
| `404` | Cidade ou registo de histórico inexistente |
| `422` | Pedido bem formado mas não processável |
| `500` | Erro interno |
| `502` | Uma das APIs da Open-Meteo falhou ou não respondeu |

---

## Correlation ID

Cada pedido recebe um `X-Correlation-Id` — reutilizado se o cliente o enviar, gerado se não.
Vai no MDC de todas as linhas de log e no corpo de todas as respostas de erro, o que permite
seguir um pedido do erro devolvido ao cliente até à causa nos ficheiros de log.

## Cache

Caffeine em memória, para não repetir chamadas às APIs externas:

| Cache | TTL | Chave |
|---|---|---|
| `geoApi` | 24 h | `cidade\|idioma\|count\|formato`, normalizada |
| `weatherApi` | 10 min | `latitude\|longitude` com 4 casas decimais (≈ 11 m) |


## Migrações

Ficam em `src/main/resources/db/migration` e correm automaticamente no arranque.

Criar uma migração nova:

```bash
make migration name=add_alguma_coisa
```

## Logs

Configurados em `src/main/resources/logback.xml`, com um ficheiro por nível em `logs/`:

| Ficheiro | Conteúdo |
|---|---|
| `application.log` | Tudo a partir de INFO, na ordem em que aconteceu |
| `info.log`, `warn.log`, `error.log` | Apenas o nível respectivo |
| `debug.log` | Vazio salvo se activares o logger de `com.bim.api_test` |


```
[weather-api] OK | elapsed=312ms | lat=-25.9653 lon=32.5892 | timezone=Africa/Maputo
[geo-api] FALHA HTTP | status=429 TOO_MANY_REQUESTS | elapsed=812ms | cidade='Maputo' ... 
```

## Testes

```bash
./mvnw test
```

Sem base de dados, sem rede e sem Docker: os controllers são testados com `@WebMvcTest` e
serviços simulados, e a lógica de negócio com Mockito.

| Classe | Cobre |
|---|---|
| `GeoApiControllerTest` | Contrato de `/api/geo-locations`, valores por omissão, mapeamento de erros |
| `WeatherControllerTest` | Os quatro endpoints de `/api/weather`, paginação, formato de `consultedAt` |
| `HealthControllerTest` | `/health` |
| `GeoWheatherServiceTest` | Selecção de localização, soft delete, ordenação e paginação do histórico |

---

## Perfis

`dev`, `demo` e `prod` — escolhidos por `APP_PROFILE`. Diferem apenas na origem das credenciais;
o comportamento da aplicação é o mesmo nos três.
