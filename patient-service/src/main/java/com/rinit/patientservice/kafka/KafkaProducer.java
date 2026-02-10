package com.rinit.patientservice.kafka;

import com.rinit.patientservice.model.Patient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Slf4j
@Service
public class KafkaProducer {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;

    }

 public void sendEvent(Patient patient){

     PatientEvent event = PatientEvent.newBuilder()
             .setPatientId(patient.getId().toString())
             .setName(patient.getName())
             .setEmail(patient.getEmail())
             .setEventType("PATIENT CREATED")
             .build();

     try {
         kafkaTemplate.send("patient",event.toByteArray());
     }catch (Exception e){
         log.error("Error sending PatientCreated even : {}" , event);
     }


 }
}
