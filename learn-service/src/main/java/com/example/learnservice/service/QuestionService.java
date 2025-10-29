package com.example.learnservice.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.learnservice.dto.AnswerDTO;
import com.example.learnservice.dto.QuestionCreateRequest;
import com.example.learnservice.dto.QuestionPositionRequest;
import com.example.learnservice.model.Answer;
import com.example.learnservice.model.Position;
import com.example.learnservice.model.Question;
import com.example.learnservice.model.QuestionPosition;
import com.example.learnservice.model.Test;
import com.example.learnservice.model.TestQuestion;
import com.example.learnservice.repository.AnswerRepository;
import com.example.learnservice.repository.QuestionPositionRepository;
import com.example.learnservice.repository.QuestionRepository;
import com.example.learnservice.repository.TestQuestionRepository;
import com.example.learnservice.repository.TestRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class QuestionService {
    @Autowired
    private TestRepository testRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private TestQuestionRepository testQuestionRepository;
    @Autowired
    private AnswerRepository answerRepository;
    @Autowired
    private PositionService positionService;
    @Autowired
    private QuestionPositionRepository questionPositionRepository;

    /*
     * Tạo câu hỏi mới
     */
    public List<Question> createQuestion(Long userId, List<QuestionCreateRequest> request) {
        List<Question> questions = new ArrayList<>();
        for (QuestionCreateRequest req : request) {
            Question question = new Question();
            question.setText(req.getText());
            question.setCreatedBy(userId);
            question = questionRepository.save(question);
            
            for (AnswerDTO dto : req.getAnswers()) {
                Answer answer = new Answer();
                answer.setQuestion(question);
                answer.setText(dto.getText());
                answer.setTrueAnswer(dto.getTrueAnswer());
                answer.setCreatedBy(userId);
                answerRepository.save(answer);
            }
            Position position = positionService.getPositionById(req.getPositionId());
            QuestionPosition qp = new QuestionPosition();
            qp.setQuestion(question);
            qp.setPosition(position);
            qp.setCreatedBy(userId);
            questionPositionRepository.save(qp);

            questions.add(questionRepository.findById(question.getId()).get());
        }
        return questions;
    }

    /*
     * Thêm câu hỏi vào bài kiểm tra
     */
    public void addQuestionToTest(Long testId, Long userId, List<Long> questionIds) {
        Test test = testRepository.findById(testId)
            .orElseThrow(() -> new RuntimeException("Test not found with id: " + testId));
        List<Question> questions = questionRepository.findAllById(questionIds);
        if (questions.isEmpty()) {
            throw new RuntimeException("No questions found with given ids");
        }
        List<TestQuestion> existingTestQuestions = testQuestionRepository.findAllByTestId(testId);
        long duration = Duration.between(test.getCreatedAt(), LocalDateTime.now()).toSeconds();
        if (duration < 30) {
            for (Question question : questions) {
            TestQuestion tq = new TestQuestion();
            tq.setTest(test);
            tq.setQuestion(question);
            tq.setCreatedBy(userId);
            testQuestionRepository.save(tq);
            }
        }
        else {
            List<Question> existingQuestions = existingTestQuestions.stream()
                .map(TestQuestion::getQuestion).toList();
            questions.removeAll(existingQuestions); // Remove questions already in the test
            if (questions.isEmpty()) {
                throw new RuntimeException("All questions are already in the test");
            }
            test.setUpdatedBy(userId);
            test = testRepository.save(test);
            
            for (Question question : questions) {
                TestQuestion tq = new TestQuestion();
                tq.setTest(test);
                tq.setQuestion(question);
                tq.setCreatedBy(userId);
                testQuestionRepository.save(tq);
            }
        }
    }

    /*
     * Xóa câu hỏi khỏi bài kiểm tra
     */
    public void removeQuestionFromTest(Long testId, Long userId, List<Long> questionIds) {
        for (Long questionId : questionIds) {
            Optional<TestQuestion> tq = testQuestionRepository.findByTestIdAndQuestionId(testId, questionId);

            if (tq.isPresent()) {
                Test test = tq.get().getTest();
                test.setUpdatedBy(userId);
                testRepository.save(test);

                testQuestionRepository.delete(tq.get());
            }
        }
    }

    /*
     * Lưu câu hỏi vào vị trí
     */
    public void assignQuestionsToPosition(Long userId, QuestionPositionRequest request) {
        Position position = positionService.getPositionById(request.getPositionId());
        for (Long questionId : request.getQuestionIds()) {
            Question question = questionRepository.findById(questionId).orElseThrow(() -> {
                log.error("Question with id {} not found", questionId);
                return new RuntimeException("Question not found");
            });
            QuestionPosition qp = new QuestionPosition();
            qp.setQuestion(question);
            qp.setPosition(position);
            qp.setCreatedBy(userId);
            questionPositionRepository.save(qp);
        }
    }

    /*
     * Xoá câu hỏi khỏi vị trí
     */
    public void deleteQuestionsFromPosition(Long userId, QuestionPositionRequest request) {
        Position position = positionService.getPositionById(request.getPositionId());
        for (Long questionId : request.getQuestionIds()) {
            Question question = questionRepository.findById(questionId).orElseThrow(() -> {
                log.error("Question with id {} not found", questionId);
                return new RuntimeException("Question not found");
            });
            QuestionPosition qp = questionPositionRepository.findByQuestionIdAndPositionId(question.getId(), position.getId()).orElseThrow(() -> {
                log.error("QuestionPosition with question id {} and position id {} not found", question.getId(), position.getId());
                return new RuntimeException("No such question in this position");
            });
            questionPositionRepository.delete(qp);
        }
    }

    /*
     * Lấy danh sách câu hỏi theo vị trí với phân trang
     */
    public Page<Question> getQuestionsByPosition(Long positionId, Pageable pageable) {
        Page<QuestionPosition> questionPage = questionPositionRepository.findAllByPositionId(positionId, pageable);

        return questionPage.map(QuestionPosition::getQuestion);
    }

    /*
     * Lấy ds câu hỏi hiện có trong 1 test
     */
    public List<Question> getQuestionsInTest(Long testId) {
        List<TestQuestion> testQuestions = testQuestionRepository.findAllByTestId(testId);
        List<Question> questions = testQuestions.stream()
                .map(TestQuestion::getQuestion).toList();
        return questions;
    }
}
