package com.example.learnservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.learnservice.dto.SemesterCreateRequest;
import com.example.learnservice.model.Semester;
import com.example.learnservice.service.SemesterService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/semester")
public class SemesterController {

    @Autowired
    private SemesterService semesterService;

    @GetMapping("/")
    public ResponseEntity<?> getListSemesters() {
        List<Semester> semesters = semesterService.getAllSemester();
        Map<String, Object> response = new HashMap<>();
        response.put("semesters", semesters);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/")
    public ResponseEntity<?> createSemester(@Valid @RequestBody SemesterCreateRequest semesterCreateRequest) {
        Semester semester = semesterService.saveSemester(semesterCreateRequest);
        Map<String, Object> response = new HashMap<>();
        response.put("semesterName", semester.getName());
        response.put("startDate", semester.getStartDate());
        response.put("endDate", semester.getEndDate());
        return ResponseEntity.ok(response);
    }
}
