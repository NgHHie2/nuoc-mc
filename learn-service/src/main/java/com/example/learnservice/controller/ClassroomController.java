package com.example.learnservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.learnservice.dto.ClassroomCreateRequest;
import com.example.learnservice.dto.ClassroomUpdateRequest;
import com.example.learnservice.model.Classroom;
import com.example.learnservice.service.ClassroomService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/semester/classroom")
public class ClassroomController {

    @Autowired
    private ClassroomService classroomService;

    @GetMapping("/")
    public ResponseEntity<?> getListClassrooms(
            @RequestParam(value = "semesterId", required = true) Long semesterId) {

        List<Classroom> classrooms = classroomService.getClassroomsBySemester(semesterId);
        Map<String, Object> response = new HashMap<>();
        response.put("classrooms", classrooms);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{classroomId}")
    public ResponseEntity<?> getClassroomById(@PathVariable Long classroomId) {
        Classroom classroom = classroomService.getClassroomById(classroomId);
        return ResponseEntity.ok(classroom);
    }

    @PostMapping("/")
    public ResponseEntity<?> createClassroom(
            @Valid @RequestBody ClassroomCreateRequest createRequest,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long userId = Long.valueOf(userIdStr);
        Classroom classroom = classroomService.createClassroom(createRequest, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", classroom.getId());
        response.put("name", classroom.getName());
        response.put("semesterId", classroom.getSemester().getId());
        response.put("semesterName", classroom.getSemester().getName());
        response.put("createdAt", classroom.getCreatedAt());

        return ResponseEntity.ok(response);

    }

    @DeleteMapping("/{classroomId}")
    public ResponseEntity<?> deleteClassroom(
            @PathVariable Long classroomId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long userId = Long.valueOf(userIdStr);
        classroomService.deleteClassroom(classroomId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Classroom deleted successfully");
        response.put("classroomId", classroomId);
        response.put("deletedAt", java.time.LocalDateTime.now());

        return ResponseEntity.ok(response);

    }
}