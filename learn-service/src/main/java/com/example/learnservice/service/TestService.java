package com.example.learnservice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import com.example.learnservice.dto.SemesterResponse;
import com.example.learnservice.dto.SemesterTestCreateRequest;
import com.example.learnservice.dto.TestCreateRequest;
import com.example.learnservice.dto.TestUpdateRequest;
import com.example.learnservice.model.Position;
import com.example.learnservice.model.SemesterTest;
import com.example.learnservice.model.Test;
import com.example.learnservice.repository.PositionRepository;
import com.example.learnservice.repository.TestRepository;
import com.example.learnservice.repository.SemeterTestRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TestService {
    @Autowired
    private TestRepository testRepository;
    @Autowired
    private PositionRepository positionRepository;
    @Autowired
    private SemeterTestRepository semesterTestRepository;

    /*
     * Tạo bài kiểm tra mới (không thuộc kỳ học cụ thể nào)
     */
    public Test createTest(TestCreateRequest request, Long userId) {
        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new RuntimeException("Position not found with id: " + request.getPositionId()));
        // Create new Test
        Test test = new Test();
        test.setName(request.getTestName());
        test.setCreatedBy(userId);
        test.setPosition(position);

        Test savedTest = testRepository.save(test);
        return savedTest;
    }

    /*
     * Lấy ds tất cả các bài kiểm tra có phân trang
     */
    // public Page<Test> getAllTests(Pageable pageable) {
    //     return testRepository.findAll(pageable);
    // }

    /*
     * Xóa bài kiểm tra theo testId
     */
    public void deleteTest(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + testId));
        testRepository.delete(test);
    }

    /*
     * Sửa bài kiểm tra theo testId (Hiện chỉ sửa mỗi tên và visible)
     */
    public Test updateTest(Long testId, TestUpdateRequest request, Long userId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + testId));

        test.setName(request.getTestName());
        test.setVisible(request.getVisible());
        test.setUpdatedBy(userId);

        Test updatedTest = testRepository.save(test);
        return updatedTest;
    }

    public List<Test> getAllTests() {
        return testRepository.findAll();
    }

    public Test getTestById(Long testId) {
        return testRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Test not found with id: " + testId));
    }
    
    public List<SemesterResponse> getSemestersUsingTest(Long testId) {
        List<SemesterTest> semesterTests = semesterTestRepository.findAllByTestId(testId);
        List<SemesterResponse> semesterResponses = semesterTests.stream().map(SemesterTest::getSemester)
                .map(s -> SemesterResponse.builder()
                        .id(s.getId())
                        .semesterName(s.getName())
                        .startDate(s.getStartDate())
                        .endDate(s.getEndDate())
                        .build())
                .toList();
        return semesterResponses;
    }
}
