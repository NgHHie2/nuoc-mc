package com.example.learnservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.learnservice.model.Answer;
import com.example.learnservice.model.Question;
import com.example.learnservice.model.Result;
import com.example.learnservice.model.SemesterTest;
import com.example.learnservice.model.TestQuestion;
import com.example.learnservice.repository.ResultRepository;
import com.example.learnservice.repository.SemeterTestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SemesterTestService {
    @Autowired
    private SemeterTestRepository semesterTestRepository;

    @Autowired
    private ResultRepository resultRepository;

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

    /**
     * Bắt đầu làm bài thi - Tạo Result
     */
    @Transactional
    public Result startTest(Long semesterTestId, Long studentId) {
        SemesterTest semesterTest = getSemesterTestById(semesterTestId);

        // Kiểm tra thời gian thi
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(semesterTest.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test has not started yet");
        }
        if (now.isAfter(semesterTest.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test has ended");
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

        return resultRepository.save(result);
    }

    /**
     * Lấy Result theo ID (full)
     */
    public Result getResultById(Long resultId) {
        return resultRepository.findById(resultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
    }

    /**
     * Lấy 1 câu hỏi cụ thể bằng JSONB operator (siêu tối ưu)
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
     * Chọn đáp án cho câu hỏi (siêu tối ưu với jsonb_set)
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
        } catch (Exception e) {
            log.error("Error updating student answers", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update answer");
        }
    }

    /**
     * Đánh flag câu hỏi (siêu tối ưu với jsonb_set)
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
     * Kiểm tra quyền truy cập Result cho Student (full object)
     */
    public void validateStudentAccess(Result result, Long studentId) {
        if (!result.getStudentId().equals(studentId)) {
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
        for (TestQuestion tq : semesterTest.getTest().getTestQuestions()) {
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

            studentAnswers.set(String.valueOf(questionIndex), answerNode);
            questionIndex++;
        }

        return studentAnswers;
    }
}