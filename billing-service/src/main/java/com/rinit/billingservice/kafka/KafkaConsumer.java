package com.rinit.billingservice.kafka;

import billing.events.BillingAccountEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    @KafkaListener(topics = "billing-account", groupId = "billing-service")
    public void consume(byte[] event){
        try{
            BillingAccountEvent billingAccountEvent = BillingAccountEvent.parseFrom(event);

            log.info("Receive billing account event: [PatientId={}, PatientName={}, PatientEmail={}]" ,
                    billingAccountEvent.getPatientId(), billingAccountEvent.getName(), billingAccountEvent.getEmail());
        }catch(InvalidProtocolBufferException e){

            log.error("Error while parsing billing account event: {}", e.getMessage());
        }

    }

}
