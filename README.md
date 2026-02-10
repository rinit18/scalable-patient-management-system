# Scalable Patient Management System

A production-grade microservices backend built with **Spring Boot**, **gRPC**, and **Apache Kafka**, demonstrating real-world distributed systems architecture, event-driven design, and infrastructure automation.

This project simulates a scalable healthcare backend where independent services communicate via synchronous (gRPC) and asynchronous (Kafka) patterns while being secured through an centralized API gateway.

---

## System Architecture

This system follows a **microservices architecture** with clear service boundaries:

Client → API Gateway → Domain Services → Messaging Layer

Core components:

- **API Gateway** — centralized routing, JWT validation, rate control
- **Auth Service** — authentication and token management
- **Patient Service** — domain CRUD operations + event publishing
- **Billing Service** — gRPC microservice for account provisioning
- **Analytics Service** — Kafka consumer for event processing
- **Infrastructure Layer** — AWS CDK Infrastructure as Code

### Communication Patterns

- REST → external client communication
- gRPC → low-latency service-to-service calls
- Kafka + Protobuf → event-driven asynchronous messaging

This hybrid architecture models real production systems used in fintech and large-scale SaaS platforms.

---

## Engineering Highlights

- Microservices with clear domain separation
- Event-driven architecture using Kafka
- Protobuf serialization for efficient messaging
- gRPC inter-service communication
- JWT-based API security via gateway
- Integration testing with RestAssured + JUnit
- Containerized services using Docker
- Infrastructure automation using AWS CDK
- Swagger/OpenAPI documentation
- Validation and exception handling strategies

This system is designed with scalability, decoupling, and fault tolerance in mind.

---

## Repository Structure

```
api-gateway/          → gateway routing and authentication
auth-service/         → user authentication and JWT issuance
patient-service/      → core domain service
billing-service/      → gRPC billing microservice
analytics-service/    → Kafka consumer service
integration-tests/    → end-to-end API tests
infrastructure/       → AWS CDK infrastructure code
api-request/          → REST request collections
grpc-request/         → gRPC request examples
```

---

## Technology Stack

- Java 17
- Spring Boot 3
- Spring Security + JWT
- Spring Cloud Gateway
- Apache Kafka
- gRPC + Protocol Buffers
- Docker
- Maven
- JUnit + RestAssured
- AWS CDK (Infrastructure as Code)

---

## Running the System Locally

### Prerequisites

- Java 17
- Maven
- Docker
- Kafka broker
- Database (H2 or PostgreSQL)
- JWT secret configuration

### Start services (recommended order)

1. auth-service
2. billing-service
3. patient-service
4. analytics-service
5. api-gateway

Run each service:

```
mvn spring-boot:run
```

---

## Integration Testing

```
mvn test
```

End-to-end tests validate authentication and patient workflows through the gateway.

---

## CI Pipeline (GitHub Actions)

The repository includes a multi-stage CI pipeline under `.github/workflows`:

- `maven.yml` builds each microservice with a Maven matrix on `push` and `pull_request` to `main`.
- `docker-build.yml` builds Docker images for each service on `push` to `main`.
- `integration-test.yml` brings up the stack with Docker Compose, waits for the gateway, runs `integration-tests`, then tears down the containers.

If you add or rename a service, update the workflow matrices so CI stays in sync.

---

## API Documentation

- Patient Service → http://localhost:4000/swagger-ui/index.html
- Auth Service → http://localhost:4005/swagger-ui/index.html

---

## Architectural Goals

This project explores:

- Scalable backend architecture design
- Service decoupling via event-driven systems
- Efficient inter-service communication
- Production-ready API security
- Infrastructure automation
- Real-world deployment workflows

It is intentionally structured to resemble enterprise backend systems used in modern distributed platforms.

---

## Future Enhancements

- Observability with Prometheus + Grafana
- Service discovery and orchestration
- Kubernetes deployment
- Circuit breakers and resilience patterns
- Load testing and performance profiling

---

## Author

Rinit Bhowmick — Backend Engineer focused on scalable distributed systems.
