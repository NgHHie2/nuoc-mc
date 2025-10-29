package com.example.learnservice.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.learnservice.annotation.RequireRole;
import com.example.learnservice.dto.QuestionCreateRequest;
import com.example.learnservice.dto.QuestionPositionRequest;
import com.example.learnservice.enums.Role;
import com.example.learnservice.model.Question;
import com.example.learnservice.service.QuestionService;
import com.example.learnservice.service.SemesterService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/semester/questions")
public class QuestionController {
    @Autowired
    private QuestionService questionService;
    @Autowired
    private SemesterService semesterService;

    /*
     * Tạo câu hỏi mới
     */
    @PostMapping()
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    public ResponseEntity<List<Question>> createQuestion(
            @Valid @RequestBody List<QuestionCreateRequest> request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdStr) {

        Long userId = Long.valueOf(userIdStr);
        return ResponseEntity.ok(questionService.createQuestion(userId, request));
    }

    /*
     * Lưu câu hỏi vào bài kiểm tra 
     * (sử dụng chung cho tạo mới bài kiểm tra và cập nhật bài kiểm tra)
     * (Phân biệt bằng hàm check thời gian tạo bài kiểm tra trong service)
     */
    @PostMapping("/tests/{testId}")
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    public String addQuestionToTest(
            @PathVariable Long testId,
            @RequestBody List<Long> questionIds,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {

        Long userId = Long.valueOf(userIdStr);
        questionService.addQuestionToTest(testId, userId, questionIds);
        return "Questions added successfully";
    }

    /*
     * Xoá câu hỏi khỏi bài kiểm tra
     */
    @DeleteMapping("/tests/{testId}")
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    public String deleteQuestionFromTest(
            @PathVariable Long testId,
            @RequestBody List<Long> questionIds,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdStr) {

        Long userId = Long.valueOf(userIdStr);
        questionService.removeQuestionFromTest(testId, userId, questionIds);
        return "Question removed from test successfully";
    }

    /*
     * Gán câu hỏi vào position
     */
    @PostMapping("/positions")
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    public String assignQuestionsToPosition(
            @Valid @RequestBody QuestionPositionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdStr) {
        Long userId = Long.valueOf(userIdStr);
        questionService.assignQuestionsToPosition(userId, request);
        return "Questions assigned to position successfully";
    }

    /*
     * Xoá câu hỏi khỏi position
     */
    @DeleteMapping("/positions")
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    public String deleteQuestionsFromPosition(
            @Valid @RequestBody QuestionPositionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdStr) {
        Long userId = Long.valueOf(userIdStr);
        questionService.deleteQuestionsFromPosition(userId, request);
        return "Questions removed from position successfully";
    }

    /*
     * Lấy danh sách câu hỏi theo positionId, có phân trang 
     */
    @GetMapping()
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    public Page<Question> getQuestionsByPosition(
            @RequestParam(value = "positionId") Long positionId,
            Pageable pageable) {
        log.info("Fetching questions for positionId: {}, page: {}, size: {}", positionId, pageable);
        return questionService.getQuestionsByPosition(positionId, pageable);
    }

    /*
     * Lấy câu hỏi hiện có trong 1 test
     */
    @GetMapping("/tests/{testId}")
    public ResponseEntity<List<Question>> getQuestionsInTest(@PathVariable Long testId) {
        log.info("Fetching questions for test {} in semester ", testId);
        return ResponseEntity.ok(questionService.getQuestionsInTest(testId));
    }
    
}
