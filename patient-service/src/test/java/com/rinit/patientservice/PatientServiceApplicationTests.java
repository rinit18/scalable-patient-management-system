package com.rinit.patientservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class PatientServiceApplicationTests {

    // This spins up a Postgres 15 container specifically for this test
    // matching your docker-compose.yml configuration exactly.
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Test
    void contextLoads() {
        // Test will now pass because Flyway can successfully create the pgcrypto extension
    }
}