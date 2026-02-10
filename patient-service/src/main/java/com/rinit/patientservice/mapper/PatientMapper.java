package com.rinit.patientservice.mapper;

import com.rinit.patientservice.dto.PatientRequestDto;
import com.rinit.patientservice.dto.PatientResponseDto;
import com.rinit.patientservice.model.Patient;

import java.time.LocalDate;

public class PatientMapper {

    public static PatientResponseDto toDto(Patient patient) {
        PatientResponseDto patientDto = new PatientResponseDto();

        patientDto.setId(patient.getId().toString());
        patientDto.setName(patient.getName());
        patientDto.setEmail(patient.getEmail());
        patientDto.setAddress(patient.getAddress());
        patientDto.setDateOfBirth(patient.getDateOfBirth().toString());


    return patientDto;
    }

    public static Patient toModel(PatientRequestDto  patientRequestDto) {

       Patient patient = new Patient();
       patient.setName(patientRequestDto.getName());
       patient.setEmail(patientRequestDto.getEmail());
       patient.setAddress(patientRequestDto.getAddress());
       patient.setDateOfBirth(LocalDate.parse(patientRequestDto.getDateOfBirth()));



        return patient;
    }
}
