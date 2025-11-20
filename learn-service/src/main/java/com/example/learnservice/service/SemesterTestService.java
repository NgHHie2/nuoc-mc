package com.example.learnservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.learnservice.controller.WebSocketController;
import com.example.learnservice.dto.ResultStatusProjection;
import com.example.learnservice.dto.SubmittedStudent;
import com.example.learnservice.dto.SemesterResponse;
import com.example.learnservice.dto.SemesterTestAssignRequest;
import com.example.learnservice.dto.SemesterTestUpdateRequest;
import com.example.learnservice.dto.SemesterTestCreateRequest;
import com.example.learnservice.enums.Role;
import com.example.learnservice.enums.TestType;
import com.example.learnservice.model.Answer;
import com.example.learnservice.model.Position;
import com.example.learnservice.model.Question;
import com.example.learnservice.model.Result;
import com.example.learnservice.model.Semester;
import com.example.learnservice.model.SemesterAccount;
import com.example.learnservice.model.SemesterTeacher;
import com.example.learnservice.model.SemesterTest;
import com.example.learnservice.model.Test;
import com.example.learnservice.model.TestQuestion;
import com.example.learnservice.repository.QuestionRepository;
import com.example.learnservice.repository.ResultRepository;
import com.example.learnservice.repository.SemesterAccountRepository;
import com.example.learnservice.repository.SemesterRepository;
import com.example.learnservice.repository.SemesterTeacherRepository;
import com.example.learnservice.repository.SemeterTestRepository;
import com.example.learnservice.repository.TestRepository;
import com.example.learnservice.util.ValidateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.ForbiddenException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SemesterTestService {
    @Autowired
    private SemeterTestRepository semesterTestRepository;

    @Autowired
    private SemesterAccountRepository semesterAccountRepository;

    @Autowired
    private SemesterTeacherRepository semesterTeacherRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private WebSocketController webSocketController;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private PositionService positionService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Lấy thông tin chi tiết của một bài thi
     */
    public SemesterTest getSemesterTestById(Long semesterTestId) {
        return semesterTestRepository.findById(semesterTestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "SemesterTest not found with id: " + semesterTestId));
    }

    /*
     * Lấy danh sách bài thi exam
     */
    public List<SemesterTest> getExamTests(Long semesterId) {
        return semesterTestRepository.findAllBySemesterIdAndType(semesterId, TestType.EXAM);
    }

    /*
     * Mở bài thi
     */
    @Transactional
    public void openTest(Long semesterTestId, Long userId, Role role) {
        // Chỉ ADMIN và TEACHER mới được mở
        if (role != Role.ADMIN && role != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        SemesterTest semesterTest = getSemesterTestById(semesterTestId);
        semesterTest.setOpen(true);
        semesterTestRepository.save(semesterTest);

        // Notify all users via WebSocket
        webSocketController.notifyTestOpened(semesterTestId);

        log.info("Test {} opened by user {}", semesterTestId, userId);
    }

    /*
     * Check test status
     */
    public Optional<ResultStatusProjection> getTestStatus(Long semesterTestId, Long studentId) {
        return resultRepository.findFirstBySemesterTestIdAndStudentIdOrderByCreatedAtDesc(
                semesterTestId, studentId);
    }

    public List<SubmittedStudent> getListSubmittedStudents(Long semesterTestId) {
        List<ResultStatusProjection> result = resultRepository.findAllBySemesterTestId(semesterTestId);
        List<SubmittedStudent> submittedStudents = result.stream()
                .filter(r -> r.getSubmitDateTime() != null)
                .map(r -> SubmittedStudent.builder()
                        .userId(r.getStudentId())
                        .score(r.getScore())
                        .resultId(r.getId())
                        .build())
                .toList();
        ;

        return submittedStudents;
    }

    /**
     * Bắt đầu làm bài thi - Tạo Result
     * Nếu là EXAM và đã có result thì trả về result hiện tại
     */
    @Transactional
    public Result startTest(Long semesterTestId, Long studentId, Role role) {
        if (role.equals(Role.STUDENT))
            validateAccessTest(semesterTestId, studentId);
        SemesterTest semesterTest = getSemesterTestById(semesterTestId);

        // Kiểm tra test đã mở chưa
        if (!semesterTest.getOpen()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test is not open yet");
        }

        // Kiểm tra thời gian thi
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(semesterTest.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test has not started yet");
        }
        if (now.isAfter(semesterTest.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test has ended");
        }

        // Nếu là EXAM, check xem đã có result chưa
        if (semesterTest.getType() == TestType.EXAM) {
            Optional<ResultStatusProjection> existingResult = resultRepository
                    .findFirstBySemesterTestIdAndStudentIdOrderByCreatedAtDesc(semesterTestId, studentId);

            if (existingResult.isPresent()) {
                ResultStatusProjection result = existingResult.get();

                // Nếu đã submit rồi thì không cho vào lại
                if (result.getSubmitDateTime() != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "You have already submitted this exam");
                }
                webSocketController.updateUserStatus(semesterTestId, studentId,
                        WebSocketController.TestStatus.TESTING);
                Result tempResult = new Result();
                tempResult.setId(result.getId());
                // Trả về result hiện tại để tiếp tục thi
                log.info("Student {} continuing exam {} with existing result {}",
                        studentId, semesterTestId, result.getId());
                return tempResult;
            }
        }

        // Tạo detailTest từ Test hiện tại
        JsonNode detailTest = buildDetailTest(semesterTest);
        JsonNode trueAnswers = buildTrueAnswers(semesterTest);
        JsonNode studentAnswers = buildStudentAnswers(semesterTest);

        // Tạo Result mới
        Result result = new Result();
        result.setSemesterTest(semesterTest);
        result.setStudentId(studentId);
        result.setDetailTest(detailTest);
        result.setTrueAnswers(trueAnswers);
        result.setStudentAnswers(studentAnswers);
        result.setStartDateTime(now);

        Result savedResult = resultRepository.save(result);

        // Update WebSocket status to TESTING
        webSocketController.updateUserStatus(semesterTestId, studentId,
                WebSocketController.TestStatus.TESTING);

        log.info("Student {} started test {} with new result {}",
                studentId, semesterTestId, savedResult.getId());

        return savedResult;
    }

    /*
     * Kết thúc một bài thi - chấm điểm
     */
    @Transactional
    public Float endTest(Long resultId, Long studentId) {
        Result result = getResultById(resultId);
        if (!result.getStudentId().equals(studentId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the summitter of this result!");
        SemesterTest semesterTest = result.getSemesterTest();

        // Kiểm tra thời gian thi
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(semesterTest.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test has not started yet");
        }
        if (now.isAfter(semesterTest.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test has ended");
        }

        result.setSubmitDateTime(now);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode trueAnswerJson = result.getTrueAnswers();
            JsonNode studentAnswerJson = result.getStudentAnswers();

            Iterator<String> fieldNames = trueAnswerJson.fieldNames();
            result.setScore((float) 0);
            while (fieldNames.hasNext()) {
                String index = fieldNames.next();

                List<Integer> trueAnswers = mapper.convertValue(trueAnswerJson.get(index),
                        mapper.getTypeFactory().constructCollectionType(List.class, Integer.class));

                JsonNode selectedAnswersNode = studentAnswerJson.get(index).get("selectedAnswers");
                List<Integer> selectedAnswers = mapper.convertValue(selectedAnswersNode,
                        mapper.getTypeFactory().constructCollectionType(List.class, Integer.class));

                boolean isSame = ValidateUtil.isSameList(trueAnswers, selectedAnswers);
                if (isSame)
                    result.setScore(result.getScore() + 1);
            }

            // Update WebSocket status to SUBMITTED
            webSocketController.notifyTestSubmitted(semesterTest.getId(), resultId, studentId, result.getScore());

            return result.getScore();
        } catch (Exception e) {
            log.error("Error while scoring" + e.getMessage());
        }

        return null;
    }

    /**
     * Lấy Result theo ID (full)
     */
    public Result getResultById(Long resultId) {
        return resultRepository.findById(resultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
    }

    /**
     * Lấy 1 câu hỏi cụ thể bằng JSONB operator
     * Query: detail_test->'questions'->0, student_answers->'0'
     */
    public Object[] getQuestionByIndex(Long resultId, Integer questionIndex, Long userId) {
        // Kiểm tra quyền truy cập trước
        validateStudentAccessLight(resultId, userId);

        String questionIndexStr = String.valueOf(questionIndex);
        return resultRepository.findQuestionByIndex(resultId, questionIndex, questionIndexStr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
    }

    /**
     * Chọn đáp án cho câu hỏi
     * UPDATE: jsonb_set(student_answers, '{0,selectedAnswers}', '[1,2]')
     */
    @Transactional
    public void selectAnswer(Long resultId, Integer questionIndex, List<Integer> answerIndices, Long userId) {
        // Kiểm tra quyền truy cập
        validateStudentAccessLight(resultId, userId);

        // Kiểm tra đã submit chưa
        Boolean isSubmitted = resultRepository.isSubmitted(resultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
        if (isSubmitted) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test already submitted");
        }

        try {
            // Build JSONB path: {0, selectedAnswers}
            String path = String.format("{\"%d\",\"selectedAnswers\"}", questionIndex);

            // Build JSONB value: [1,2,3]
            ArrayNode valueArray = objectMapper.createArrayNode();
            if (answerIndices != null) {
                answerIndices.forEach(valueArray::add);
            }
            String value = objectMapper.writeValueAsString(valueArray);

            // Update bằng jsonb_set
            int updated = resultRepository.updateStudentAnswerPath(resultId, path, value);
            if (updated == 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update answer");
            }

            // Build JSONB path for timestamp: {0, answeredAt}
            String pathTimestamp = String.format("{\"%d\",\"answeredAt\"}", questionIndex);

            // Get current timestamp in ISO format
            String timestamp = String.format("\"%s\"", LocalDateTime.now().toString());

            // Update timestamp
            resultRepository.updateStudentAnswerPath(resultId, pathTimestamp, timestamp);

            log.info("Student {} selected answers {} for question {} at {}",
                    userId, answerIndices, questionIndex, timestamp);
        } catch (Exception e) {
            log.error("Error updating student answers", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update answer");
        }
    }

    /**
     * Đánh flag câu hỏi
     * UPDATE: jsonb_set(student_answers, '{0,flagged}', 'true')
     */
    @Transactional
    public void flagQuestion(Long resultId, Integer questionIndex, Boolean flagged, Long userId) {
        // Kiểm tra quyền truy cập
        validateStudentAccessLight(resultId, userId);

        // Kiểm tra đã submit chưa
        Boolean isSubmitted = resultRepository.isSubmitted(resultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
        if (isSubmitted) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test already submitted");
        }

        try {
            // Build JSONB path: {0, flagged}
            String path = String.format("{\"%d\",\"flagged\"}", questionIndex);

            // Build JSONB value: true/false
            String value = String.valueOf(flagged);

            // Update bằng jsonb_set
            int updated = resultRepository.updateFlaggedPath(resultId, path, value);
            if (updated == 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update flag");
            }
        } catch (Exception e) {
            log.error("Error updating flag", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update flag");
        }
    }

    /**
     * Lấy danh sách kết quả của user trong một bài thi
     */
    public List<Result> getMyResultsBySemesterTest(Long semesterTestId, Long userId) {
        return resultRepository.findBySemesterTestIdAndStudentId(semesterTestId, userId);
    }

    /**
     * Đếm số câu đã trả lời (dùng JSONB query)
     */
    public Integer countAnsweredQuestions(Long resultId) {
        return resultRepository.countAnsweredQuestions(resultId);
    }

    /**
     * Đếm số câu đã flag (dùng JSONB query)
     */
    public Integer countFlaggedQuestions(Long resultId) {
        return resultRepository.countFlaggedQuestions(resultId);
    }

    /**
     * Kiểm tra quyền truy cập Result cho Student (lightweight)
     */
    public void validateStudentAccessLight(Long resultId, Long studentId) {
        Long ownerId = resultRepository.findStudentIdById(resultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));

        if (!ownerId.equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    /**
     * Xây dựng detailTest từ SemesterTest (dùng index thay vì ID)
     */
    private JsonNode buildDetailTest(SemesterTest semesterTest) {
        ObjectNode detailTest = objectMapper.createObjectNode();

        detailTest.put("testName", semesterTest.getTest().getName());
        detailTest.put("semesterTestName", semesterTest.getName());
        detailTest.put("startDate", semesterTest.getStartDate().toString());
        detailTest.put("endDate", semesterTest.getEndDate().toString());

        ArrayNode questionsArray = objectMapper.createArrayNode();

        int questionIndex = 0;
        Test test = testRepository.findWithQuestionsById(semesterTest.getTest().getId())
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));
        List<Long> questionIds = test.getTestQuestions().stream()
                .map(tq -> tq.getQuestion().getId())
                .distinct()
                .toList();

        if (!questionIds.isEmpty())
            questionRepository.findAllWithAnswers(questionIds);
        // Test test = semesterTest.getTest();
        for (TestQuestion tq : test.getTestQuestions()) {
            Question question = tq.getQuestion();
            ObjectNode questionNode = objectMapper.createObjectNode();
            questionNode.put("questionIndex", questionIndex);
            questionNode.put("questionText", question.getText());

            ArrayNode answersArray = objectMapper.createArrayNode();
            int answerIndex = 0;
            for (Answer answer : question.getAnswers()) {
                ObjectNode answerNode = objectMapper.createObjectNode();
                answerNode.put("answerIndex", answerIndex);
                answerNode.put("answerText", answer.getText());
                answersArray.add(answerNode);
                answerIndex++;
            }

            questionNode.set("answers", answersArray);
            questionsArray.add(questionNode);
            questionIndex++;
        }

        detailTest.set("questions", questionsArray);

        return detailTest;
    }

    /**
     * Xây dựng trueAnswers từ SemesterTest (dùng index)
     */
    private JsonNode buildTrueAnswers(SemesterTest semesterTest) {
        ObjectNode trueAnswers = objectMapper.createObjectNode();

        int questionIndex = 0;
        for (TestQuestion tq : semesterTest.getTest().getTestQuestions()) {
            Question question = tq.getQuestion();

            ArrayNode correctAnswerIndices = objectMapper.createArrayNode();
            int answerIndex = 0;
            for (Answer answer : question.getAnswers()) {
                if (answer.getTrueAnswer() != null && answer.getTrueAnswer()) {
                    correctAnswerIndices.add(answerIndex);
                }
                answerIndex++;
            }

            trueAnswers.set(String.valueOf(questionIndex), correctAnswerIndices);
            questionIndex++;
        }

        return trueAnswers;
    }

    /**
     * Xây dựng studentAnswers với giá trị null ban đầu
     */
    private JsonNode buildStudentAnswers(SemesterTest semesterTest) {
        ObjectNode studentAnswers = objectMapper.createObjectNode();

        int questionIndex = 0;
        for (TestQuestion tq : semesterTest.getTest().getTestQuestions()) {
            ObjectNode answerNode = objectMapper.createObjectNode();
            answerNode.set("selectedAnswers", objectMapper.createArrayNode());
            answerNode.put("flagged", false);
            answerNode.putNull("answeredAt");

            studentAnswers.set(String.valueOf(questionIndex), answerNode);
            questionIndex++;
        }

        return studentAnswers;
    }

    public SemesterTest validateAccessTest(Long semesterTestId, Long studentId) {
        SemesterTest semesterTest = semesterTestRepository.findById(semesterTestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));
        SemesterAccount semesterAccount = semesterAccountRepository
                .findBySemesterIdAndAccountId(semesterTest.getSemester().getId(), studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
        if (!semesterTest.getTest().getPosition().getId().equals(semesterAccount.getPosition().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return semesterTest;
    }

    public SemesterTest validateAccessTestWithTeacher(Long semesterTestId, Long teacherId) {
        SemesterTest semesterTest = semesterTestRepository.findById(semesterTestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));
        boolean existing = semesterTeacherRepository
                .existsBySemesterIdAndTeacherId(semesterTest.getSemester().getId(), teacherId);
        if (!existing) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return semesterTest;
    }

    /*
     * Tạo bài kiểm tra mới trong kỳ học
     */
    public Test createTest(Long semesterId, SemesterTestCreateRequest request, Long userId) {
        // Create new Test
        Test test = new Test();
        test.setName(request.getTestName());
        test.setCreatedBy(userId);
        test.setUpdatedBy(userId);
        Position position = positionService.getPositionById(request.getPositionId());
        test.setPosition(position);

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new RuntimeException("Semester not found with id: " + semesterId));

        Test savedTest = testRepository.save(test);

        SemesterTest semesterTest = new SemesterTest();
        semesterTest.setName(request.getTestName());
        semesterTest.setStartDate(request.getStartDate());
        semesterTest.setEndDate(request.getEndDate());
        semesterTest.setType(request.getType());
        semesterTest.setMinutes(request.getMinutes());
        semesterTest.setSemester(semester);
        semesterTest.setTest(savedTest);
        semesterTest.setCreatedBy(userId);
        semesterTestRepository.save(semesterTest);

        return savedTest;
    }

    /*
     * Lấy tất cả bài kiểm tra trong kỳ học
     */
    public List<SemesterTest> getAllTestsInSemester(Long semesterId) {
        return semesterTestRepository.findAllBySemesterId(semesterId);
    }

    /*
     * Xóa bài kiểm tra khỏi kỳ học
     */
    public void deleteTestFromSemester(Long semesterId, Long testId) {
        Optional<SemesterTest> semesterTest = semesterTestRepository.findBySemesterIdAndTestId(semesterId, testId);
        if (semesterTest.isEmpty()) {
            throw new RuntimeException("Test with id " + testId + " not found in semester with id " + semesterId);
        }

        semesterTestRepository.delete(semesterTest.get());
    }

    /*
     * Lấy tất cả bài kiểm tra có position phù hợp mà chưa được thêm vào trong kỳ
     * học
     */
    public List<Test> getAvailableTestsWithPosition(Long semesterId, Long positionId) {
        List<Test> testsInSemester = getAllTestsInSemester(semesterId).stream().map(SemesterTest::getTest).toList();
        List<Long> testsIdInSemester = testsInSemester.stream().map(Test::getId).toList();
        if (testsIdInSemester.isEmpty()) {
            return testRepository.findAllByPositionId(positionId);
        }
        return testRepository.findAllByPositionId(positionId).stream()
                .filter(test -> !testsIdInSemester.contains(test.getId())).toList();
    }

    /*
     * Cập nhật thông tin SemesterTest
     */
    public SemesterTest updateSemesterTestInfo(Long semesterId, Long testId, Long userId,
            SemesterTestUpdateRequest request) {
        SemesterTest semesterTest = semesterTestRepository.findBySemesterIdAndTestId(semesterId, testId)
                .orElseThrow(() -> new RuntimeException("SemesterTest not found"));
        semesterTest.setName(request.getName());
        semesterTest.setStartDate(request.getStartDate());
        semesterTest.setEndDate(request.getEndDate());
        semesterTest.setMinutes(request.getMinutes());
        semesterTest.setUpdatedBy(userId);
        semesterTestRepository.save(semesterTest);
        return semesterTest;
    }

    /*
     * Lấy ra danh sách các Semester sử dụng 1 Test (Không tính semester đang sử
     * dụng)
     */
    public List<SemesterResponse> getSemestersUsingTest(Long testId, Long semesterId) {
        List<SemesterTest> semesterTests = semesterTestRepository.findAllByTestId(testId);
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new RuntimeException("Semester not found with id: " + semesterId));

        List<SemesterResponse> semesterResponses = semesterTests.stream()
                .map(SemesterTest::getSemester)
                .filter(s -> !s.getId().equals(semesterId))
                .map(s -> SemesterResponse.builder()
                        .id(s.getId())
                        .semesterName(s.getName())
                        .startDate(s.getStartDate())
                        .endDate(s.getEndDate())
                        .build())
                .toList();
        return semesterResponses;
    }

    /*
     * Lấy thông tin 1 bài kiểm tra trong kỳ học
     */
    public SemesterTest getTestInSemester(Long semesterId, Long testId) {
        return semesterTestRepository.findBySemesterIdAndTestId(semesterId, testId)
                .orElseThrow(() -> new RuntimeException("SemesterTest not found"));
    }

    /*
     * Gán bài kiểm tra vào kỳ học
     */
    public String assignTestToSemester(Long semesterId, Long testId, Long userId, SemesterTestAssignRequest request) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new RuntimeException("Semester not found with id: " + semesterId));
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + testId));
        // Kiểm tra xem bài kiểm tra đã được gán vào kỳ học chưa
        Optional<SemesterTest> existingSemesterTest = semesterTestRepository.findBySemesterIdAndTestId(semesterId,
                testId);
        if (existingSemesterTest.isPresent()) {
            throw new RuntimeException(
                    "Test with id " + testId + " is already assigned to semester with id " + semesterId);
        }
        SemesterTest semesterTest = new SemesterTest();
        semesterTest.setName(request.getName());
        semesterTest.setStartDate(request.getStartDate());
        semesterTest.setEndDate(request.getEndDate());
        semesterTest.setMinutes(request.getMinutes());
        semesterTest.setType(request.getType());
        semesterTest.setSemester(semester);
        semesterTest.setTest(test);
        semesterTest.setCreatedBy(userId);
        semesterTestRepository.save(semesterTest);
        return "Test assigned to semester successfully";
    }
}
