package com.example.learnservice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.learnservice.dto.ClassroomCreateRequest;
import com.example.learnservice.dto.ClassroomUpdateRequest;
import com.example.learnservice.model.Classroom;
import com.example.learnservice.model.Semester;
import com.example.learnservice.repository.ClassroomRepository;
import com.example.learnservice.repository.SemesterRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClassroomService {

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    public List<Classroom> getAllClassrooms() {
        return classroomRepository.findAll();
    }

    public List<Classroom> getClassroomsBySemester(Long semesterId) {
        return classroomRepository.findBySemesterId(semesterId);
    }

    public Classroom getClassroomById(Long classroomId) {
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        if (classroomOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom not found with id: " + classroomId);
        }
        return classroomOpt.get();
    }

    @Transactional
    public Classroom createClassroom(ClassroomCreateRequest createRequest, Long userId) {
        // Kiểm tra semester có tồn tại không
        Optional<Semester> semesterOpt = semesterRepository.findById(createRequest.getSemesterId());
        if (semesterOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Semester not found with id: " + createRequest.getSemesterId());
        }

        Semester semester = semesterOpt.get();

        Classroom classroom = new Classroom();
        classroom.setName(createRequest.getName());
        classroom.setSemester(semester);
        classroom.setCreatedBy(userId);
        classroom.setUpdatedBy(userId);

        return classroomRepository.save(classroom);
    }

    @Transactional
    public void deleteClassroom(Long classroomId, Long userId) {
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        if (classroomOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom not found with id: " + classroomId);
        }

        Classroom classroom = classroomOpt.get();

        // Check if classroom has students or documents
        if (classroom.getClassroomAccounts() != null && !classroom.getClassroomAccounts().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete classroom with existing students. Please remove all students first.");
        }

        if (classroom.getClassroomDocuments() != null && !classroom.getClassroomDocuments().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete classroom with existing documents. Please remove all documents first.");
        }

        log.info("Deleting classroom: {} (ID: {}) by user: {}",
                classroom.getName(), classroom.getId(), userId);

        classroomRepository.delete(classroom);

        log.info("Classroom deleted successfully: {}", classroomId);
    }
}