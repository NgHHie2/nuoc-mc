package com.example.learnservice.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.learnservice.model.Subject;
import com.example.learnservice.repository.SubjectRepository;

@Service
public class SubjectService {
    @Autowired
    private SubjectRepository subjectRepository;

    public Optional<Subject> getSubjectById(int id) {
        return subjectRepository.findById(id);
    }
    
}
