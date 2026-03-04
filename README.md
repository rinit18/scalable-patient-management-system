# Scalable Patient Management System

A **production-style microservices backend** built with **Spring Boot**, **Apache Kafka**, and **gRPC**, demonstrating **distributed system design, event-driven architecture, and infrastructure automation**.

The system simulates a scalable healthcare backend where independent services communicate using **synchronous RPC (gRPC)** and **asynchronous messaging (Kafka)** behind a **secured API Gateway**.

---

# Architecture Overview

```
Client
   │
API Gateway (JWT validation + Redis rate limiting)
   │
   ├── Auth Service
   │      Authentication & JWT issuance
   │
   ├── Patient Service
   │      Core domain logic
   │      ├─ Kafka producer
   │      ├─ Redis cache
   │      └─ Metrics instrumentation
   │
   ├── Billing Service
   │      gRPC billing account provisioning
   │
   └── Analytics Service
          Kafka consumer for event processing
```

Infrastructure stack:

```
AWS CDK • Docker Compose • Prometheus • Grafana
```

---

# Service Communication Patterns

| Pattern      | Usage                               |
| ------------ | ----------------------------------- |
| REST / HTTP  | Client → API Gateway                |
| gRPC         | Patient Service → Billing Service   |
| Kafka Events | Patient Service → Analytics Service |

---

# Key Engineering Highlights

* **Microservices architecture** with independent PostgreSQL databases per service
* **Event-driven system design** using Apache Kafka with Protobuf serialization
* **Reliable Kafka producer configuration** (`acks=all`, retries enabled)
* **Low-latency service communication** using gRPC
* **Redis caching layer** for frequently accessed patient records
* **API rate limiting** implemented at the gateway using Redis
* **Circuit breaker & retry patterns** implemented with Resilience4j
* **JWT authentication** enforced through a custom Spring Cloud Gateway filter
* **Custom Micrometer metrics (AOP)** tracking Redis cache misses
* **Centralized exception handling** with structured error responses
* **Observability stack** using Prometheus, Grafana, and Spring Boot Actuator
* **Containerized microservices** with optimized multi-stage Docker builds
* **Infrastructure as Code** using AWS CDK
* **CI/CD pipelines** implemented with GitHub Actions
* **End-to-end integration testing** using RestAssured + JUnit

---

# Repository Structure

```
api-gateway/         API gateway (JWT validation, Redis rate limiting)
auth-service/        Authentication service
patient-service/     Core domain service (REST, Kafka, Redis, metrics)
billing-service/     gRPC billing microservice
analytics-service/   Kafka consumer service

integration-tests/   End-to-end API tests
infrastructure/      AWS CDK infrastructure code
monitoring/          Prometheus configuration
api-request/         REST request collections
grpc-request/        gRPC request examples
```

---

# Technology Stack

| Category         | Technology                      |
| ---------------- | ------------------------------- |
| Language         | Java 17                         |
| Framework        | Spring Boot 3                   |
| Security         | Spring Security + JWT           |
| Gateway          | Spring Cloud Gateway            |
| Messaging        | Apache Kafka + Protocol Buffers |
| RPC              | gRPC                            |
| Caching          | Redis                           |
| Database         | PostgreSQL                      |
| Resilience       | Resilience4j                    |
| Metrics          | Micrometer                      |
| Monitoring       | Prometheus + Grafana            |
| Containerization | Docker + Docker Compose         |
| Build Tool       | Maven                           |
| Testing          | JUnit 5 + RestAssured           |
| Infrastructure   | AWS CDK                         |
| CI/CD            | GitHub Actions                  |

---

# Running the System Locally

## Prerequisites

* Docker
* Docker Compose
* Java 17 (optional if running services locally)

## Start all services

```bash
docker-compose up --build
```

This command starts:

* all microservices
* PostgreSQL databases
* Kafka + Zookeeper
* Redis

---

# Service Ports

| Service           | Port                      |
| ----------------- | ------------------------- |
| API Gateway       | 4004                      |
| Auth Service      | 4005                      |
| Patient Service   | 4000                      |
| Billing Service   | 4001 (HTTP) / 9001 (gRPC) |
| Analytics Service | 4002                      |
| Redis             | 6379                      |

---

# API Documentation

| Service         | URL                                         |
| --------------- | ------------------------------------------- |
| Patient Service | http://localhost:4000/swagger-ui/index.html |
| Auth Service    | http://localhost:4005/swagger-ui/index.html |

---

# Integration Testing

Run full end-to-end tests:

```bash
mvn test
```

Tests validate authentication and patient workflows through the **API Gateway**.

---

# CI/CD Pipeline

Three automated workflows run on `push` and `pull_request` events.

| Workflow               | Purpose                                          |
| ---------------------- | ------------------------------------------------ |
| `maven.yml`            | Builds all services                              |
| `docker-build.yml`     | Builds Docker images                             |
| `integration-test.yml` | Runs full integration tests using Docker Compose |

---

# Observability

Metrics are exposed through **Spring Boot Actuator + Micrometer**.

Prometheus scrapes metrics every **5 seconds**.

Grafana dashboards visualize:

* API latency
* request throughput
* cache hit / miss ratio
* service health metrics

Example custom metric:

```
custom.redis.cache.miss
```

Tracked via a **Micrometer AOP aspect**.

---

# Future Improvements

* Kubernetes deployment manifests
* Distributed tracing with OpenTelemetry
* Service discovery integration
* Load testing and performance benchmarking

---

# Author

**Rinit Bhowmick**
Backend developer focused on **distributed systems, microservices architecture, and scalable backend infrastructure**.
