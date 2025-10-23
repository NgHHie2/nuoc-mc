package com.example.learnservice.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import com.example.learnservice.annotation.RequireRole;
import com.example.learnservice.client.AccountClient;
import com.example.learnservice.dto.AccountDTO;
import com.example.learnservice.dto.ApiResponse;
import com.example.learnservice.dto.EndTestResponse;
import com.example.learnservice.dto.QuestionResponse;
import com.example.learnservice.dto.ResultDetailDTO;
import com.example.learnservice.dto.ResultSummaryDTO;
import com.example.learnservice.dto.SelectAnswerRequest;
import com.example.learnservice.dto.StartTestResponse;
import com.example.learnservice.enums.Role;
import com.example.learnservice.model.Result;
import com.example.learnservice.model.SemesterTest;
import com.example.learnservice.service.SemesterTestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/semester")
public class SemesterTestController {

    @Autowired
    private SemesterTestService semesterTestService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Lấy thông tin chi tiết của một bài thi
     */
    @GetMapping("/test/{semesterTestId}")
    @RequireRole({ Role.ADMIN, Role.TEACHER, Role.STUDENT })
    public ResponseEntity<SemesterTest> getSemesterTestById(
            @PathVariable Long semesterTestId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {

        Long studentId = Long.valueOf(userIdStr);
        Role userRole = Role.valueOf(userRoleStr);
        if (userRole.equals(Role.STUDENT)) {
            SemesterTest semesterTest = semesterTestService.validateAccessTest(semesterTestId, studentId);
            return ResponseEntity.ok(semesterTest);
        }
        SemesterTest semesterTest = semesterTestService.getSemesterTestById(semesterTestId);
        return ResponseEntity.ok(semesterTest);
    }

    /**
     * Mở bài thi - chỉ ADMIN và TEACHER
     */
    @PostMapping("/test/{semesterTestId}/open")
    @RequireRole({ Role.ADMIN, Role.TEACHER })
    public ResponseEntity<ApiResponse> openTest(
            @PathVariable Long semesterTestId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {

        Long userId = Long.valueOf(userIdStr);
        Role userRole = Role.valueOf(userRoleStr);

        semesterTestService.openTest(semesterTestId, userId, userRole);

        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage("Test opened successfully");

        log.info("User {} opened test {}", userId, semesterTestId);

        return ResponseEntity.ok(response);
    }

    /**
     * Bắt đầu làm bài thi - Tạo Result
     */
    @PostMapping("/test/{semesterTestId}/start")
    @RequireRole({ Role.ADMIN, Role.TEACHER, Role.STUDENT })
    public ResponseEntity<StartTestResponse> startTest(
            @PathVariable Long semesterTestId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {

        Long studentId = Long.valueOf(userIdStr);
        Role userRole = Role.valueOf(userRoleStr);
        Result result = semesterTestService.startTest(semesterTestId, studentId, userRole);

        // Map sang DTO
        StartTestResponse response = new StartTestResponse();
        response.setSuccess(true);
        response.setResultId(result.getId());
        response.setMessage("Started test successfully");
        // response.setTotalQuestions(((ArrayNode)
        // result.getDetailTest().get("questions")).size());

        log.info("Student {} started test {} with result {}", studentId, semesterTestId, result.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * Kết thúc bài thi - Chấm điểm
     */
    @PostMapping("/test/{resultId}/end")
    @RequireRole({ Role.ADMIN, Role.TEACHER, Role.STUDENT })
    public ResponseEntity<EndTestResponse> endTest(
            @PathVariable Long resultId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {

        Long studentId = Long.valueOf(userIdStr);
        Float score = semesterTestService.endTest(resultId, studentId);

        // Map sang DTO
        EndTestResponse response = new EndTestResponse();
        response.setSuccess(true);
        response.setResultId(resultId);
        response.setScore(score);
        response.setMessage("Ended test successfully");
        // response.setTotalQuestions(((ArrayNode)
        // result.getDetailTest().get("questions")).size());

        log.info("Student {} ended test with result {}", studentId, resultId);

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy thông tin 1 câu hỏi cụ thể
     * Query: detail_test->'questions'->:index, student_answers->':index'
     * 
     */
    @GetMapping("/test/result/{resultId}/question/{questionIndex}")
    @RequireRole({ Role.ADMIN, Role.TEACHER, Role.STUDENT })
    public ResponseEntity<QuestionResponse> getQuestion(
            @PathVariable Long resultId,
            @PathVariable Integer questionIndex,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr)
            throws JsonMappingException, JsonProcessingException {

        Long userId = Long.valueOf(userIdStr);
        Role userRole = Role.valueOf(userRoleStr);

        // Dùng JSONB operator - chỉ query đúng 1 phần tử trong array
        Object[] data = semesterTestService.getQuestionByIndex(resultId, questionIndex, userId);
        System.out.println(Arrays.deepToString(data));

        // Parse từ native query result
        Object[] row = (Object[]) data[0];
        String questionJson = (String) row[0];
        String studentAnswerJson = (String) row[1];
        if (questionJson == null || studentAnswerJson == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");

        JsonNode question = objectMapper.readTree(questionJson);
        JsonNode studentAnswer = objectMapper.readTree(studentAnswerJson);

        // Map sang DTO
        QuestionResponse response = new QuestionResponse();
        response.setQuestionIndex(questionIndex);
        response.setQuestionText(question.get("questionText").asText());

        // Parse answers
        List<QuestionResponse.AnswerOption> answers = new ArrayList<>();
        ArrayNode answersArray = (ArrayNode) question.get("answers");
        for (JsonNode answerNode : answersArray) {
            QuestionResponse.AnswerOption option = new QuestionResponse.AnswerOption();
            option.setAnswerIndex(answerNode.get("answerIndex").asInt());
            option.setAnswerText(answerNode.get("answerText").asText());
            answers.add(option);
        }
        response.setAnswers(answers);

        // Parse selected answers
        List<Integer> selectedAnswers = new ArrayList<>();
        ArrayNode selectedArray = (ArrayNode) studentAnswer.get("selectedAnswers");
        for (JsonNode selected : selectedArray) {
            selectedAnswers.add(selected.asInt());
        }
        response.setSelectedAnswers(selectedAnswers);
        response.setFlagged(studentAnswer.get("flagged").asBoolean());

        return ResponseEntity.ok(response);

    }

    /**
     * Chọn đáp án cho câu hỏi
     * UPDATE: jsonb_set(student_answers, '{0,selectedAnswers}', '[1,2]')
     */
    @PutMapping("/test/result/{resultId}/question/{questionIndex}/answer")
    @RequireRole({ Role.ADMIN, Role.TEACHER, Role.STUDENT })
    public ResponseEntity<ApiResponse> selectAnswer(
            @PathVariable Long resultId,
            @PathVariable Integer questionIndex,
            @Valid @RequestBody SelectAnswerRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {

        Long studentId = Long.valueOf(userIdStr);
        semesterTestService.selectAnswer(resultId, questionIndex, request.getAnswerIndices(), studentId);

        // Map sang DTO
        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage("Answer updated successfully");

        log.info("Student {} selected answers {} for question {} in result {}",
                studentId, request.getAnswerIndices(), questionIndex, resultId);

        return ResponseEntity.ok(response);
    }

    /**
     * Đánh flag/unflag câu hỏi
     * UPDATE: jsonb_set(student_answers, '{0,flagged}', 'true')
     */
    @PutMapping("/test/result/{resultId}/question/{questionIndex}/flag")
    @RequireRole({ Role.ADMIN, Role.TEACHER, Role.STUDENT })
    public ResponseEntity<ApiResponse> flagQuestion(
            @PathVariable Long resultId,
            @PathVariable Integer questionIndex,
            @RequestParam Boolean flagged,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {

        Long studentId = Long.valueOf(userIdStr);
        semesterTestService.flagQuestion(resultId, questionIndex, flagged, studentId);

        // Map sang DTO
        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage("Question flag updated successfully");

        log.info("Student {} {} question {} in result {}",
                studentId, flagged ? "flagged" : "unflagged", questionIndex, resultId);

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách kết quả của user trong một bài thi
     */
    @GetMapping("/test/{semesterTestId}/my-results")
    @RequireRole({ Role.ADMIN, Role.TEACHER, Role.STUDENT })
    public ResponseEntity<List<ResultSummaryDTO>> getMyResults(
            @PathVariable Long semesterTestId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {

        Long userId = Long.valueOf(userIdStr);

        // Dùng custom query - chỉ lấy results của userId
        List<Result> results = semesterTestService.getMyResultsBySemesterTest(semesterTestId, userId);

        // Map sang DTO
        List<ResultSummaryDTO> response = results.stream()
                .map(result -> {
                    ResultSummaryDTO dto = new ResultSummaryDTO();
                    dto.setId(result.getId());
                    dto.setStudentId(result.getStudentId());
                    dto.setStartDateTime(result.getStartDateTime());
                    dto.setSubmitDateTime(result.getSubmitDateTime());
                    dto.setScore(result.getScore());
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy chi tiết một kết quả
     */
    @GetMapping("/test/result/{resultId}")
    @RequireRole({ Role.ADMIN, Role.TEACHER, Role.STUDENT })
    public ResponseEntity<ResultDetailDTO> getResultDetail(
            @PathVariable Long resultId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {

        Long userId = Long.valueOf(userIdStr);
        Role userRole = Role.valueOf(userRoleStr);

        Result result = semesterTestService.getResultById(resultId);

        // Kiểm tra quyền: STUDENT chỉ xem được result của mình
        if (userRole == Role.STUDENT) {
            semesterTestService.validateStudentAccessLight(result.getId(), userId);
        }

        // Map sang DTO
        ResultDetailDTO dto = new ResultDetailDTO();
        dto.setId(result.getId());
        dto.setStudentId(result.getStudentId());
        dto.setStartDateTime(result.getStartDateTime());
        dto.setSubmitDateTime(result.getSubmitDateTime());
        dto.setScore(result.getScore());
        dto.setDetailTest(result.getDetailTest());
        dto.setStudentAnswers(result.getStudentAnswers());
        dto.setMinutes(result.getSemesterTest().getMinutes());

        // Chỉ hiển thị trueAnswers cho ADMIN/TEACHER hoặc sau khi STUDENT đã submit
        if (userRole == Role.ADMIN || userRole == Role.TEACHER
                || (userRole == Role.STUDENT && result.getSubmitDateTime() != null)) {
            dto.setTrueAnswers(result.getTrueAnswers());
        }

        return ResponseEntity.ok(dto);
    }
}