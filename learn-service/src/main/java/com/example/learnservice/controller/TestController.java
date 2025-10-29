package com.example.learnservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.learnservice.annotation.RequireRole;
import com.example.learnservice.dto.SemesterResponse;
import com.example.learnservice.dto.SemesterTestCreateRequest;
import com.example.learnservice.dto.TestCreateRequest;
import com.example.learnservice.dto.TestUpdateRequest;
import com.example.learnservice.enums.Role;
import com.example.learnservice.model.Semester;
import com.example.learnservice.model.Test;
import com.example.learnservice.service.TestService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/semester/tests")
public class TestController {
    @Autowired
    private TestService testService;

    /*
     * Test controller sử dụng để tạo các API liên quan đến bài kiểm tra mà không thuộc về kỳ học cụ thể nào (dùng cho Admin)
     */

     /*
      * Tạo bài kiểm tra mới (không thuộc kỳ học cụ thể nào)
      */
    @PostMapping
    @RequireRole({ Role.ADMIN, Role.TEACHER })
    public ResponseEntity<Test> createTest(@Valid @RequestBody TestCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {
        Long userId = Long.valueOf(userIdStr);
        return ResponseEntity.ok(testService.createTest(request, userId));
    }

    /*
     * Lấy ds tất cả các bài kiểm tra có phân trang
     */
    @GetMapping
    @RequireRole({ Role.ADMIN, Role.TEACHER })
    public ResponseEntity<List<Test>> getAllTests(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr
    ) {
        return ResponseEntity.ok(testService.getAllTests());
    }

    /*
     * Xóa bài kiểm tra theo testId
     */
    @DeleteMapping("/{testId}")
    @RequireRole({ Role.ADMIN, Role.TEACHER })
    public String deleteTest(@PathVariable Long testId) {
        testService.deleteTest(testId);
        return "Test deleted successfully";
    }

    /*
     * Sửa bài kiểm tra theo testId (Hiện chỉ sửa mỗi tên và visible)
     */
    @PutMapping("/{testId}")
    @RequireRole({ Role.ADMIN, Role.TEACHER })
    public ResponseEntity<Test> updateTest(@PathVariable Long testId,
            @RequestBody TestUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long userId = Long.valueOf(userIdStr);
        return ResponseEntity.ok(testService.updateTest(testId, request, userId));
    }

    /*
     * Lấy thông tin bài kiểm tra theo testId
     */
    @GetMapping("/{testId}")
    @RequireRole({ Role.ADMIN, Role.TEACHER })
    public ResponseEntity<Test> getTestById(@PathVariable Long testId) {
        return ResponseEntity.ok(testService.getTestById(testId));
    }

    /*
     * Lấy danh sách các semester sử dụng 1 test
     */
    @GetMapping("/{testId}/semesters")
    @RequireRole({ Role.ADMIN, Role.TEACHER })
    public ResponseEntity<List<SemesterResponse>> getSemesterUsingTest(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr,
            @PathVariable Long testId) {
        Long userId = Long.valueOf(userIdStr);
        return ResponseEntity.ok(testService.getSemestersUsingTest(testId));
    }
}
