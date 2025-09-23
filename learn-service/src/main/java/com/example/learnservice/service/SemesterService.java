package com.example.learnservice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.learnservice.dto.SemesterCreateRequest;
import com.example.learnservice.model.Semester;
import com.example.learnservice.repository.SemesterRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SemesterService {
    @Autowired
    private SemesterRepository semesterRepository;

    public List<Semester> getAllSemester() {
        return semesterRepository.findAll();
    }

    public Semester saveSemester(SemesterCreateRequest semesterCreateRequest) {
        Semester semester = new Semester();
        semester.setName(semesterCreateRequest.getName());
        if (semesterCreateRequest.getEndDate().isBefore(semesterCreateRequest.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate is before startDate");
        }
        semester.setStartDate(semesterCreateRequest.getStartDate());
        semester.setEndDate(semesterCreateRequest.getEndDate());
        return semesterRepository.save(semester);
    }
}
