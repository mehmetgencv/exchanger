
# Exchanger

Exchanger is a Spring Boot-based REST API that provides real-time currency exchange rates, conversion services, transaction history, and bulk CSV file processing. It is containerized with Docker and ready for deployment.

## Features

- Get current exchange rates between currencies
- Convert currency with unique transaction tracking
- Query historical conversion data by ID or date
- Upload CSV files for bulk conversions
- External exchange rate API integration (CurrencyLayer)
- Containerized with Docker and Docker Compose

## Extensibility

The application is built with modularity in mind. If a different exchange rate provider needs to be used instead of CurrencyLayer, you can add a new implementation by extending the `ExchangeRateClient` interface and updating the provider selector in the configuration.

All logic for exchange rate retrieval is abstracted behind interfaces and uses configuration-driven dependency resolution.

- Easily pluggable architecture for multiple providers
- New APIs can be integrated without changing controller or service layers


## Technologies

- Java 21
- Spring Boot 3
- PostgreSQL
- Maven
- Docker & Docker Compose
- H2 (for testing)
- MapStruct
- Swagger (OpenAPI)

## Getting Started

### Prerequisites

- Docker
- Docker Compose

### Running the Application

1. Clone the repository:

```bash
git clone https://github.com/mehmetgencv/exchanger.git
cd exchanger
````

2. Set your CurrencyLayer API key in the `docker-compose.yml` file:

```yaml
environment:
  EXCHANGE_API_KEY: your_currencylayer_api_key_here
```

3. Build and start the application:

```bash
docker-compose up --build
```

4. Access the API documentation:

```
http://localhost:8080/swagger-ui.html
```

## API Endpoints

* `GET /api/v1/conversion/exchange-rate`: Get single exchange rate
* `POST /api/v1/conversion/convert`: Convert currency
* `GET /api/v1/conversion/history`: Query conversion history
* `POST /api/v1/conversion/bulk`: Upload CSV for bulk conversion

## File Format for Bulk Upload

CSV file should contain:

```csv
amount,sourceCurrency,targetCurrency
100,USD,EUR
250,GBP,TRY
```

A sample CSV file is available at:
`src/main/resources/static/sample-bulk.csv`

[sample-bulk.csv](src/main/resources/static/sample-bulk.csv)
### Performance Optimization

When a CSV file is uploaded, the system groups conversion requests by `sourceCurrency` and performs a **single batched exchange rate API call** per group.
This minimizes external API usage, reduces network overhead, and improves overall performance for large bulk uploads.
## Testing

To run tests locally:

```bash
./mvnw clean verify
```

Application uses H2 in-memory DB for test profile.

## License

This project is licensed under the MIT License.

