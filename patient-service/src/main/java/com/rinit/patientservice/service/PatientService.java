package com.rinit.patientservice.service;

import com.rinit.patientservice.dto.PagedPatientResponseDto;
import com.rinit.patientservice.dto.PatientRequestDto;
import com.rinit.patientservice.dto.PatientResponseDto;
import com.rinit.patientservice.exception.EmailAlreadyExistsException;
import com.rinit.patientservice.exception.PatientNotFoundException;
import com.rinit.patientservice.grpc.BillingServiceGrpcClient;
import com.rinit.patientservice.kafka.KafkaProducer;
import com.rinit.patientservice.mapper.PatientMapper;
import com.rinit.patientservice.model.Patient;
import com.rinit.patientservice.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;

    public PatientService(PatientRepository patientRepository, BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer) {

        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;

    }


    @Cacheable(
            value = "patients",
            key = "#page + '-' + #size + '-' + #sort + '-' + #sortField",
            condition = "#searchValue == ''"

    )
    public PagedPatientResponseDto getpatients(int page, int size, String sort, String sortField, String searchValue) {

        log.info("[REDIS]: Cache miss - fetching from db");

        try{
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }

        Pageable pageable = PageRequest.of(page - 1, size, sort.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending());

        Page<Patient> patientPage;

        if (searchValue == null || searchValue.isBlank()) {

            patientPage = patientRepository.findAll(pageable);

        } else {

            patientPage = patientRepository.findByNameContainingIgnoreCase(searchValue, pageable);
        }

        List<PatientResponseDto> patientResponseDtos = patientPage.getContent()
                .stream()
                .map(PatientMapper::toDto)
                .toList();

        return new PagedPatientResponseDto(
                patientResponseDtos,
                patientPage.getNumber() +1,
                patientPage.getSize(),
                patientPage.getTotalPages(),
                (int)patientPage.getTotalElements()

        );

    }

    public PatientResponseDto createPatient(PatientRequestDto patientRequestDto){

        if(patientRepository.existsByEmail(patientRequestDto.getEmail())){
            throw new EmailAlreadyExistsException("A patient with this email" + "already exists" + patientRequestDto.getEmail());

        }

        Patient newpatient = patientRepository.save(PatientMapper.toModel(patientRequestDto));

        billingServiceGrpcClient.createBillingAccount(newpatient.getId().toString(), newpatient.getName(), newpatient.getEmail());

        kafkaProducer.sendEvent(newpatient);

        return PatientMapper.toDto(newpatient);
    }

    public PatientResponseDto updatePatient(UUID id, PatientRequestDto patientRequestDto){

        Patient patient = patientRepository.findById(id).orElseThrow(()-> new PatientNotFoundException("Patient not found with ID:"+ id));

        if(patientRepository.existsByEmailAndIdNot(patientRequestDto.getEmail(), id)){
            throw new EmailAlreadyExistsException("A patient with this email" + "already exists" + patientRequestDto.getEmail());

        }

        patient.setName(patientRequestDto.getName());
        patient.setEmail(patientRequestDto.getEmail());
        patient.setAddress(patientRequestDto.getAddress());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDto.getDateOfBirth()));


       Patient UpdatedPatient = patientRepository.save(patient);
        return PatientMapper.toDto(UpdatedPatient);
    }

    public void deletePatient(UUID id){
        patientRepository.deleteById(id);
    }
}
