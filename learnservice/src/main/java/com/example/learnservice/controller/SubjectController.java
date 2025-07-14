package com.example.learnservice.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.learnservice.model.Subject;
import com.example.learnservice.service.SubjectService;

@RestController
@RequestMapping("/subject")
public class SubjectController {
    @Autowired
    private SubjectService subjectService;

    @GetMapping("/{id}")
    public Optional<Subject> getSubjectById(@PathVariable int id) {
        return subjectService.getSubjectById(id);
    }
}
