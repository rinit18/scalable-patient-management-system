# Patient Management Pro

Patient Management Pro is a Spring Boot microservices workspace that combines REST, gRPC, and Kafka to manage patients, authenticate users, and provision billing accounts. It includes an API gateway, multiple services, request collections, and integration tests.

## Workspace layout

- `analytics-service/`: Kafka consumer for patient events (analytics/logging).
- `api-gateway/`: Spring Cloud Gateway with JWT validation filter and route definitions.
- `auth-service/`: JWT-based authentication service with a seeded admin user.
- `billing-service/`: gRPC service for billing account creation.
- `patient-service/`: REST API for CRUD patients, Kafka producer, and gRPC client.
- `api-request/`: HTTP request examples (IntelliJ HTTP client).
- `grpc-request/`: gRPC request examples (IntelliJ HTTP client).
- `integration-tests/`: RestAssured + JUnit integration tests.
- `Db_volumes/`: Local volume folders for DBs (used outside this repo).

## Architecture and flow

1) Client calls API Gateway on port 4004.
2) Gateway routes:
   - `/auth/**` -> `auth-service:4005` (no JWT filter).
   - `/api/patients/**` -> `patient-service:4000` (JWT validation required).
3) `auth-service` validates user credentials and returns a JWT.
4) `patient-service` on create:
   - Persists patient via JPA.
   - Calls `billing-service` over gRPC to create a billing account.
   - Produces a `PatientEvent` to Kafka topic `patient`.
5) `analytics-service` consumes the `patient` topic and logs the event.

## Services

### api-gateway

- Purpose: Single entry point and JWT validation.
- Port: `4004`.
- Main files:
  - `api-gateway/src/main/resources/application.yml` (routes).
  - `api-gateway/src/main/java/com/rinit/apigateway/filter/JwtValidationGatewayFilterFactory.java`
  - `api-gateway/src/main/java/com/rinit/apigateway/exception/JwtValidationException.java`
- JWT validation behavior:
  - Expects `Authorization: Bearer <token>`.
  - Calls `GET /validate` on `auth-service`.
  - Returns `401` if token is missing/invalid.

Required configuration:

- `auth.service.url` (used by `JwtValidationGatewayFilterFactory`).
  - Example: `http://localhost:4005` for local runs or `http://auth-service:4005` for Docker network.

### auth-service

- Purpose: Authenticate users and issue/validate JWTs.
- Port: `4005`.
- Dependencies: Spring Security, Spring Data JPA, JJWT, Springdoc.
- Main files:
  - `auth-service/src/main/java/com/rinit/authservice/controller/AuthController.java`
  - `auth-service/src/main/java/com/rinit/authservice/service/AuthService.java`
  - `auth-service/src/main/java/com/rinit/authservice/util/JwtUtil.java`
  - `auth-service/src/main/resources/data.sql` (seed user)

Data model:

- `User` (table: `users`):
  - `id` (UUID)
  - `email` (unique, required)
  - `password` (BCrypt hash)
  - `role`

Seed data:

- `auth-service/src/main/resources/data.sql` creates table and inserts:
  - Email: `testuser@test.com`
  - Password (plaintext used in tests): `password123`
  - Role: `ADMIN`

Endpoints:

- `POST /login` -> returns `{"token": "<jwt>"}` on success, `401` on failure.
- `GET /validate` -> requires `Authorization: Bearer <token>`, returns `200` if valid, `401` if invalid.

JWT details:

- Algorithm: HMAC (secret key loaded via `jwt.secret`).
- Claims: `sub` (email), `role`.
- Expiration: 10 hours from issuance.

Required configuration:

- `jwt.secret` must be provided. It is Base64-decoded before use.

### patient-service

- Purpose: CRUD operations on patients, emits Kafka events, calls billing gRPC.
- Port: `4000`.
- Dependencies: Spring Web, JPA, Validation, Kafka, gRPC, Springdoc.
- Main files:
  - `patient-service/src/main/java/com/rinit/patientservice/controller/PatientController.java`
  - `patient-service/src/main/java/com/rinit/patientservice/service/PatientService.java`
  - `patient-service/src/main/java/com/rinit/patientservice/grpc/BillingServiceGrpcClient.java`
  - `patient-service/src/main/java/com/rinit/patientservice/kafka/KafkaProducer.java`
  - `patient-service/src/main/resources/data.sql` (seed patients)
  - `patient-service/src/main/resources/application.properties`

Data model:

- `Patient`:
  - `id` (UUID, generated)
  - `name` (required)
  - `email` (required, unique)
  - `address` (required)
  - `dateOfBirth` (required, LocalDate)
  - `registeredDate` (auto-set on create)

Validation rules:

- `name`: required, max 100 chars.
- `email`: required, valid format.
- `address`: required.
- `dateOfBirth`: required, string parsed as ISO date (YYYY-MM-DD).

Endpoints:

- `GET /patients` -> list of `PatientResponseDto`.
- `POST /patients` -> creates a patient.
- `PUT /patients/{id}` -> updates a patient.
- `DELETE /patients/{id}` -> deletes a patient.

Error handling:

- Validation errors return `400` with field error map.
- Email already exists -> `400` with `{"email":"Email already exists"}`.
- Patient not found -> `400` with `{"patient":"Patient not found"}`.

Side effects on create:

- gRPC call to billing: `BillingService/CreateBillingAccount`.
- Kafka event on topic `patient` with `PatientEvent` payload.

Required configuration:

- `spring.kafka.bootstrap-servers` (defaults set to `kafka:9092`).
- `billing.service.address` (default `localhost`).
- `billing.service.grpc.port` (default `9001`).

Database configuration:

- `spring.datasource.*` not set by default.
- H2 in-memory config is present but commented in `application.properties`.
- `data.sql` seeds patients; for external DBs, set `spring.sql.init.mode=always`.

### billing-service

- Purpose: gRPC service that returns a billing account for a patient.
- Ports:
  - HTTP: `4001` (server port)
  - gRPC: `9001` (grpc server port)
- Main files:
  - `billing-service/src/main/java/com/rinit/billingservice/grpc/BillingGrpcService.java`
  - `billing-service/src/main/proto/billing_service.proto`

gRPC API:

```
service BillingService {
  rpc CreateBillingAccount (BillingRequest) returns (BillingResponse);
}
```

BillingResponse returned:

- `accountId`: set to patientId.
- `status`: `ACTIVE`.

### analytics-service

- Purpose: Kafka consumer for `patient` topic.
- Port: no `server.port` configured (Spring Boot defaults to `8080`), Dockerfile exposes `4002`.
- Main files:
  - `analytics-service/src/main/java/com/rinit/analyticsservice/kafka/KafkaConsumer.java`
  - `analytics-service/src/main/proto/patient_event.proto`

Kafka behavior:

- `@KafkaListener(topics = "patient", groupId = "analytics-service")`
- Expects `PatientEvent` protobuf payload.
- Logs patientId, name, email.

Required configuration:

- `spring.kafka.bootstrap-servers` must be set in environment or defaults.

### integration-tests

- Purpose: end-to-end validation of gateway + auth + patient.
- Tooling: RestAssured + JUnit 5.
- Base URI: `http://localhost:4004`.
- Tests:
  - `AuthIntegrationTest` validates login success and failure.
  - `PatientIntegrationTest` logs in and calls `/api/patients`.

Run:

```
mvn test
```

## Kafka event schema

`patient_event.proto` (used by patient-service and analytics-service):

```
message PatientEvent {
  string patientId = 1;
  string name = 2;
  string email = 3;
  string event_type = 4;
}
```

Topic:

- `patient`
- Producer: `patient-service`
- Consumer: `analytics-service`

## REST API examples

### Auth

Login:

```
POST http://localhost:4004/auth/login
Content-Type: application/json

{
  "email": "testuser@test.com",
  "password": "password123"
}
```

Validate:

```
GET http://localhost:4004/auth/validate
Authorization: Bearer <token>
```

### Patients

Create:

```
POST http://localhost:4004/api/patients
Content-Type: application/json

{
  "name": "Soham Das",
  "email": "soham.das.dev04@example.com",
  "address": "Uluberia, Kolkata, West Bengal",
  "dateOfBirth": "1999-06-14"
}
```

Get all:

```
GET http://localhost:4004/api/patients
Authorization: Bearer <token>
```

Update (direct service example from requests):

```
PUT http://localhost:4000/api/patients/<patient-id>
Content-Type: application/json

{
  "name": "Lord Bhowmick",
  "email": "lord@gmail.com",
  "address": "uluberia kolkata",
  "dateOfBirth": "1999-06-13"
}
```

Delete (direct service example from requests):

```
DELETE http://localhost:4000/api/patients/<patient-id>
```

Request collections live in:

- `api-request/auth-service/`
- `api-request/patient-service/`

## gRPC request example

```
GRPC localhost:9001/BillingService/CreateBillingAccount

{
  "patientId": "12345",
  "name": "Rinit Bhowmick",
  "email": "rbbsl13@gmail.com"
}
```

Example lives in `grpc-request/billing-service/create-billing-account.http`.

## OpenAPI docs

- Patient service: `http://localhost:4000/swagger-ui/index.html` and `http://localhost:4000/v3/api-docs`
- Auth service: `http://localhost:4005/swagger-ui/index.html` and `http://localhost:4005/v3/api-docs`
- Gateway passthrough:
  - `http://localhost:4004/api-docs/patients`
  - `http://localhost:4004/api-docs/auth`

## Build and run

### Prerequisites

- JDK 17 (services) and Maven.
- Kafka broker reachable at `spring.kafka.bootstrap-servers` (default `kafka:9092`).
- Database (Postgres or H2) for `auth-service` and `patient-service`.
- `jwt.secret` set for `auth-service`.

### Run services locally (Maven)

From each service directory:

```
mvn spring-boot:run
```

Recommended startup order:

1) `auth-service`
2) `billing-service`
3) `patient-service`
4) `analytics-service`
5) `api-gateway`

### Build JARs

```
mvn clean package -DskipTests
```

### Docker images

Each service has a multi-stage Dockerfile:

```
docker build -t patient-service ./patient-service
docker build -t auth-service ./auth-service
docker build -t billing-service ./billing-service
docker build -t analytics-service ./analytics-service
docker build -t api-gateway ./api-gateway
```

You will need to supply network configuration and environment variables (JWT secret, DB, Kafka, service URLs).

## Notes and mismatches to be aware of

- `analytics-service` does not set `server.port`, so it defaults to `8080` even though the Dockerfile exposes `4002`.
- `api-gateway` expects `auth.service.url` to be configured; it is not set in `application.yml`.
- `data.sql` seeding for auth/patient only runs automatically with embedded DBs unless `spring.sql.init.mode=always` is set.
#   s c a l a b l e - p a t i e n t - m a n a g e m e n t - s y s t e m  
 