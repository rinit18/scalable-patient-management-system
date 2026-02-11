package com.rinit.patientservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PagedPatientResponseDto {
    private List<PatientResponseDto> patients;
    private int page;
    private int size;
    private int totalPages;
    private int totalElements;

    public PagedPatientResponseDto() {
    }

    public PagedPatientResponseDto(List<PatientResponseDto> patients, int page, int size, int totalPages, int totalElements) {
        this.patients = patients;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }


}


