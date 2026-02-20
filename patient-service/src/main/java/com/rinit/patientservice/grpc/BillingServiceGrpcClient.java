package com.rinit.patientservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import com.rinit.patientservice.kafka.KafkaProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BillingServiceGrpcClient {

    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;
    private final KafkaProducer kafkaProducer;

    //localhost:9001/BillingService/CreatePatientAccount
    // for production aws.grpc:123123/BillingService/CreatePatientAccount
    public BillingServiceGrpcClient(@Value("${billing.service.address:localhost}") String serverAddress,
                                    @Value("${billing.service.grpc.port:9001}") int serverPort, KafkaProducer kafkaProducer) {


        log.info("Connecting to Billing Service GRPC service at {}:{}", serverAddress, serverPort);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort).usePlaintext().build();

        blockingStub = BillingServiceGrpc.newBlockingStub(channel);
        this.kafkaProducer = kafkaProducer;


    }

    @CircuitBreaker(name="billingService", fallbackMethod = "billingFallback")
    @Retry(name = "billingRetry")
    public BillingResponse createBillingAccount(String patientId, String name, String email){

        BillingRequest request = BillingRequest.newBuilder().setPatientId(patientId).setName(name).setEmail(email).build();

        BillingResponse response = blockingStub.createBillingAccount(request);
        log.info("Received response from billing service via GRpc: {}", response);

        return response;


    }

    public BillingResponse billingFallback(String patientId, String name, String email, Throwable t){


        log.warn("[CIRCUIT BREAKER]: Billing service is unavailable. Triggered" + "fallback: {}", t.getMessage());

        kafkaProducer.sendBillingAccountEvent(patientId, name , email);

        return BillingResponse.newBuilder()
                .setAccountId("")
                .setStatus("PENDING")
                .build();
    }
}
