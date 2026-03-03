# Scalable Patient Management System

A production-grade microservices backend built with **Spring Boot**, **gRPC**, and **Apache Kafka**, demonstrating real-world distributed systems architecture, event-driven design, and infrastructure automation.

This project simulates a scalable healthcare backend where independent services communicate via synchronous (gRPC) and asynchronous (Kafka) patterns while being secured through a centralized API Gateway.

---

## System Architecture

```
Client
  └── API Gateway (JWT validation, Redis rate limiting)
        ├── Auth Service      (authentication, JWT issuance)
        ├── Patient Service   (CRUD, Kafka producer, Redis cache, Metrics AOP)
        │     └── Billing Service (gRPC account provisioning)
        └── Analytics Service (Kafka consumer, event processing)

Infrastructure: AWS CDK · Docker Compose · Prometheus · Grafana
```

### Communication Patterns

| Pattern     | Usage                                      |
|-------------|--------------------------------------------|
| REST / HTTP | External client ↔ API Gateway              |
| gRPC        | Patient Service → Billing Service          |
| Kafka       | Patient Service → Analytics Service        |

---

## Engineering Highlights

- **Microservices** with clear domain separation and independent PostgreSQL databases per service
- **Event-driven architecture** using Apache Kafka + Protobuf serialization
- **Kafka producer durability** — `acks=all` and retry configured for guaranteed delivery
- **gRPC** for low-latency inter-service calls (Patient → Billing)
- **Redis caching** on patient reads to reduce database load
- **Redis-backed rate limiting** at the API Gateway (IP-based, via Spring Cloud Gateway)
- **Circuit breaker & retry** (Resilience4j) protecting the gRPC call to Billing Service
- **Custom JWT gateway filter** — validates Bearer tokens against Auth Service using WebClient
- **Custom Micrometer AOP metrics** — tracks Redis cache misses as custom counters
- **Global exception handling** with typed, structured error responses
- **Prometheus + Grafana** observability stack via Spring Boot Actuator
- **CI/CD** with multi-stage GitHub Actions pipelines
- **Integration testing** with RestAssured + JUnit end-to-end
- **Containerized** with optimized multi-stage Dockerfiles
- **Infrastructure as Code** using AWS CDK

---

## Repository Structure

```
api-gateway/          → JWT validation filter, Redis rate limiter, route config
auth-service/         → user authentication and JWT issuance
patient-service/      → core domain service (REST, Kafka, gRPC, Redis, Metrics)
billing-service/      → gRPC billing microservice
analytics-service/    → Kafka consumer and event processor
integration-tests/    → end-to-end API tests (RestAssured + JUnit)
infrastructure/       → AWS CDK infrastructure code
monitoring/           → Prometheus scrape configuration
api-request/          → REST request collections
grpc-request/         → gRPC request examples
```

---

## Technology Stack

| Category         | Technology                                       |
|------------------|--------------------------------------------------|
| Language         | Java 17                                          |
| Framework        | Spring Boot 3                                    |
| Security         | Spring Security + JWT (JJWT 0.12)                |
| Gateway          | Spring Cloud Gateway                             |
| Messaging        | Apache Kafka + Protocol Buffers                  |
| RPC              | gRPC 1.69                                        |
| Caching          | Redis (Spring Cache)                             |
| Rate Limiting    | Redis Rate Limiter (Spring Cloud Gateway)        |
| Resilience       | Resilience4j (Circuit Breaker + Retry)           |
| Persistence      | PostgreSQL (per service)                         |
| Metrics          | Micrometer + Custom AOP Aspects                  |
| Monitoring       | Prometheus + Grafana + Spring Boot Actuator      |
| Containerization | Docker + Docker Compose                          |
| Build Tool       | Maven                                            |
| Testing          | JUnit 5 + RestAssured                            |
| IaC              | AWS CDK                                          |

---

## Running the System Locally

### Prerequisites

- Docker & Docker Compose
- Java 17 + Maven (only for running services outside Docker)

### Quickstart with Docker Compose

```bash
docker-compose up --build
```

This starts all services, databases, Kafka, Zookeeper, and Redis automatically.

### Service Ports

| Service           | Port                       |
|-------------------|----------------------------|
| API Gateway       | 4004                       |
| Auth Service      | 4005                       |
| Patient Service   | 4000                       |
| Billing Service   | 4001 (HTTP) · 9001 (gRPC)  |
| Analytics Service | 4002                       |
| Auth DB (PG)      | 5001                       |
| Patient DB (PG)   | 5000                       |
| Redis             | 6379                       |

### Manual Service Startup Order

If running services individually (outside Docker):

1. auth-service
2. billing-service
3. patient-service
4. analytics-service
5. api-gateway

```bash
mvn spring-boot:run
```

---

## API Documentation (Swagger UI)

| Service         | URL                                         |
|-----------------|---------------------------------------------|
| Patient Service | http://localhost:4000/swagger-ui/index.html |
| Auth Service    | http://localhost:4005/swagger-ui/index.html |

---

## Integration Testing

```bash
mvn test
```

End-to-end tests validate authentication and patient workflows through the API Gateway.

---

## CI Pipeline (GitHub Actions)

Three automated workflows run on `push` and `pull_request` to `main`:

| Workflow               | Trigger          | Action                                             |
|------------------------|------------------|----------------------------------------------------|
| `maven.yml`            | push / PR → main | Builds each service in a matrix (skips tests)      |
| `docker-build.yml`     | push → main      | Builds Docker images for all services              |
| `integration-test.yml` | push → main      | Spins up full stack via Docker Compose, runs tests |

> If you add or rename a service, update the workflow matrices to keep CI in sync.

---

## Monitoring

Prometheus and Grafana are configured for real-time observability:

- **Prometheus** scrapes metrics from the Patient Service via Spring Boot Actuator (`/actuator/prometheus`) every **5 seconds** — configured in `monitoring/prometheus.yml`.
- **Grafana** connects to Prometheus for dashboard visualization of service metrics.
- **Custom AOP metric** — `custom.redis.cache.miss` counter is tracked via a Micrometer aspect on the patient list endpoint.

Actuator endpoints exposed: `health`, `info`, `prometheus`, `metrics`, `cache`.

---

## Architectural Goals

- Scalable microservice backend design
- Service decoupling via event-driven messaging
- Efficient inter-service communication with gRPC
- Production-ready API security, rate limiting, and token management
- Infrastructure automation with AWS CDK
- Full observability with Prometheus, Grafana, and custom metrics

---

## Future Enhancements

- Service discovery with Kubernetes / Consul
- Kubernetes deployment manifests
- Distributed tracing with OpenTelemetry
- Load testing and performance profiling

---

## Author

**Rinit Bhowmick** — Backend Engineer focused on scalable distributed systems.
