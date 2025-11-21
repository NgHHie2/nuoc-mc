package com.example.learnservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.example.learnservice.config.TestConfig;

import com.example.learnservice.dto.SelectAnswerRequest;
import com.example.learnservice.enums.TestType;
import com.example.learnservice.model.Answer;
import com.example.learnservice.model.Position;
import com.example.learnservice.model.Question;
import com.example.learnservice.model.Result;
import com.example.learnservice.model.Semester;
import com.example.learnservice.model.SemesterAccount;
import com.example.learnservice.model.SemesterTest;
import com.example.learnservice.model.TestQuestion;
import com.example.learnservice.repository.AnswerRepository;
import com.example.learnservice.repository.PositionRepository;
import com.example.learnservice.repository.QuestionRepository;
import com.example.learnservice.repository.ResultRepository;
import com.example.learnservice.repository.SemesterAccountRepository;
import com.example.learnservice.repository.SemesterRepository;
import com.example.learnservice.repository.SemeterTestRepository;
import com.example.learnservice.repository.TestQuestionRepository;
import com.example.learnservice.repository.TestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Integration test cho SemesterTestController với H2 database
 * Test các API: getSubmittedStudents, getQuestion, selectAnswer, flagQuestion,
 * getMyResults, getResultDetail, getTestExams
 */
@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
@Transactional
class SemesterTestControllerQuestionResultDbTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private SemeterTestRepository semesterTestRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private SemesterAccountRepository semesterAccountRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private TestQuestionRepository testQuestionRepository;

    @Autowired
    private ResultRepository resultRepository;

    private Semester semester;
    private Position position;
    private com.example.learnservice.model.Test test;
    private SemesterTest semesterTest;
    private Question question1;
    private Question question2;
    private Result studentResult;
    private Long studentId = 100L;
    private Long student2Id = 101L;
    private Long teacherId = 200L;
    private Long adminId = 1L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clear all data
        resultRepository.deleteAll();
        testQuestionRepository.deleteAll();
        answerRepository.deleteAll();
        questionRepository.deleteAll();
        semesterTestRepository.deleteAll();
        testRepository.deleteAll();
        semesterAccountRepository.deleteAll();
        semesterRepository.deleteAll();
        positionRepository.deleteAll();

        // Create Position
        position = new Position();
        position.setName("Software Engineer");
        position = positionRepository.save(position);

        // Create Semester
        semester = new Semester();
        semester.setName("Semester 2024");
        semester.setStartDate(LocalDateTime.now().minusDays(30));
        semester.setEndDate(LocalDateTime.now().plusDays(60));
        semester.setCreatedBy(adminId);
        semester = semesterRepository.save(semester);

        // Create Test
        test = new com.example.learnservice.model.Test();
        test.setName("Midterm Test");
        test.setPosition(position);
        test.setVisible(true);
        test.setCreatedBy(adminId);
        test = testRepository.save(test);

        // Create Questions with Answers
        question1 = createQuestionWithAnswers("What is 2 + 2?",
                new String[] { "3", "4", "5" },
                new boolean[] { false, true, false });

        question2 = createQuestionWithAnswers("What is the capital of France?",
                new String[] { "London", "Paris", "Berlin" },
                new boolean[] { false, true, false });

        // Add questions to test
        addQuestionToTest(test, question1);
        addQuestionToTest(test, question2);

        // Create SemesterTest
        semesterTest = new SemesterTest();
        semesterTest.setName("Midterm Exam");
        semesterTest.setSemester(semester);
        semesterTest.setTest(test);
        semesterTest.setType(TestType.EXAM);
        semesterTest.setMinutes(60);
        semesterTest.setStartDate(LocalDateTime.now().minusHours(1));
        semesterTest.setEndDate(LocalDateTime.now().plusHours(2));
        semesterTest.setOpen(true);
        semesterTest.setCreatedBy(adminId);
        semesterTest = semesterTestRepository.save(semesterTest);

        // Create SemesterAccount for students
        createSemesterAccountForStudent(studentId);
        createSemesterAccountForStudent(student2Id);

        // Create Result for student
        studentResult = createResultForStudent(studentId);
    }

    private Question createQuestionWithAnswers(String questionText, String[] answerTexts, boolean[] correctAnswers) {
        Question question = new Question();
        question.setText(questionText);
        question.setCreatedBy(adminId);
        question = questionRepository.save(question);

        List<Answer> answers = new ArrayList<>();
        for (int i = 0; i < answerTexts.length; i++) {
            Answer answer = new Answer();
            answer.setQuestion(question);
            answer.setText(answerTexts[i]);
            answer.setTrueAnswer(correctAnswers[i]);
            answer.setCreatedBy(adminId);
            answers.add(answerRepository.save(answer));
        }

        question.setAnswers(answers);
        return question;
    }

    private void addQuestionToTest(com.example.learnservice.model.Test test, Question question) {
        TestQuestion testQuestion = new TestQuestion();
        testQuestion.setTest(test);
        testQuestion.setQuestion(question);
        testQuestion.setCreatedBy(adminId);
        testQuestionRepository.save(testQuestion);
    }

    private void createSemesterAccountForStudent(Long accountId) {
        SemesterAccount semesterAccount = new SemesterAccount();
        semesterAccount.setSemester(semester);
        semesterAccount.setAccountId(accountId);
        semesterAccount.setPosition(position);
        semesterAccount.setCreatedBy(adminId);
        semesterAccountRepository.save(semesterAccount);
    }

    private Result createResultForStudent(Long accountId) {
        Result result = new Result();
        result.setSemesterTest(semesterTest);
        result.setStudentId(accountId);
        result.setStartDateTime(LocalDateTime.now().minusMinutes(30));

        // Create detail test
        ObjectNode detailTest = objectMapper.createObjectNode();
        ArrayNode questions = objectMapper.createArrayNode();

        ObjectNode q0 = objectMapper.createObjectNode();
        q0.put("questionIndex", 0);
        q0.put("questionText", "What is 2 + 2?");
        ArrayNode ans0 = objectMapper.createArrayNode();
        ans0.add(objectMapper.createObjectNode().put("answerIndex", 0).put("answerText", "3"));
        ans0.add(objectMapper.createObjectNode().put("answerIndex", 1).put("answerText", "4"));
        ans0.add(objectMapper.createObjectNode().put("answerIndex", 2).put("answerText", "5"));
        q0.set("answers", ans0);
        questions.add(q0);

        ObjectNode q1 = objectMapper.createObjectNode();
        q1.put("questionIndex", 1);
        q1.put("questionText", "What is the capital of France?");
        ArrayNode ans1 = objectMapper.createArrayNode();
        ans1.add(objectMapper.createObjectNode().put("answerIndex", 0).put("answerText", "London"));
        ans1.add(objectMapper.createObjectNode().put("answerIndex", 1).put("answerText", "Paris"));
        ans1.add(objectMapper.createObjectNode().put("answerIndex", 2).put("answerText", "Berlin"));
        q1.set("answers", ans1);
        questions.add(q1);

        detailTest.set("questions", questions);
        result.setDetailTest(detailTest);

        // Create student answers
        ObjectNode studentAnswers = objectMapper.createObjectNode();

        ObjectNode answer0 = objectMapper.createObjectNode();
        answer0.set("selectedAnswers", objectMapper.createArrayNode().add(1));
        answer0.put("flagged", false);
        answer0.putNull("answeredAt");
        studentAnswers.set("0", answer0);

        ObjectNode answer1 = objectMapper.createObjectNode();
        answer1.set("selectedAnswers", objectMapper.createArrayNode());
        answer1.put("flagged", true);
        answer1.putNull("answeredAt");
        studentAnswers.set("1", answer1);

        result.setStudentAnswers(studentAnswers);

        // Create true answers
        ObjectNode trueAnswers = objectMapper.createObjectNode();
        trueAnswers.set("0", objectMapper.createArrayNode().add(1));
        trueAnswers.set("1", objectMapper.createArrayNode().add(1));
        result.setTrueAnswers(trueAnswers);

        return resultRepository.save(result);
    }

    /*
     * ===============================================
     * TEST: GET /semester/test/{semesterTestId}/submitted-students
     * ===============================================
     */

    /*
     * 1. Admin có thể lấy danh sách học sinh đã nộp bài
     * - Given: Có học sinh đã nộp bài
     * - When: Admin gửi request
     * - Then: Trả về danh sách học sinh và điểm, status 200
     */
    @Test
    @DisplayName("Admin can get list of submitted students")
    void testGetSubmittedStudents_AdminSuccess() throws Exception {
        // Submit result for student
        studentResult.setSubmitDateTime(LocalDateTime.now());
        studentResult.setScore(8.5f);
        resultRepository.save(studentResult);

        // Create and submit result for student2
        Result student2Result = createResultForStudent(student2Id);
        student2Result.setSubmitDateTime(LocalDateTime.now());
        student2Result.setScore(9.0f);
        resultRepository.save(student2Result);

        mockMvc.perform(get("/semester/test/" + semesterTest.getId() + "/submitted-students")
                .header("X-User-Id", adminId.toString())
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").exists())
                .andExpect(jsonPath("$[0].score").exists())
                .andExpect(jsonPath("$[0].resultId").exists());
    }

    /*
     * 2. Trả về danh sách rỗng khi không có ai nộp bài
     * - When: Admin gửi request nhưng chưa có ai nộp
     * - Then: Trả về array rỗng, status 200
     */
    @Test
    @DisplayName("Returns empty list when no students submitted")
    void testGetSubmittedStudents_EmptyList() throws Exception {
        mockMvc.perform(get("/semester/test/" + semesterTest.getId() + "/submitted-students")
                .header("X-User-Id", adminId.toString())
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /*
     * 3. Student không thể xem danh sách học sinh đã nộp
     * - When: Student gửi request
     * - Then: Trả về 403 Forbidden
     */
    @Test
    @DisplayName("Student cannot get submitted students list")
    void testGetSubmittedStudents_StudentForbidden() throws Exception {
        mockMvc.perform(get("/semester/test/" + semesterTest.getId() + "/submitted-students")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());
    }

    /*
     * ===============================================
     * TEST: GET /semester/test/result/{resultId}/question/{questionIndex}
     * ===============================================
     */

    /*
     * 4. Student có thể lấy câu hỏi theo index
     * - Given: Student có result với câu hỏi
     * - When: Student gửi request lấy câu hỏi index 0
     * - Then: Trả về thông tin câu hỏi, đáp án đã chọn, status 200
     */
    @Test
    @DisplayName("Student can get question by index")
    void testGetQuestion_StudentSuccess() throws Exception {
        mockMvc.perform(get("/semester/test/result/" + studentResult.getId() + "/question/0")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionIndex").value(0))
                .andExpect(jsonPath("$.questionText").value("What is 2 + 2?"))
                .andExpect(jsonPath("$.answers").isArray())
                .andExpect(jsonPath("$.answers.length()").value(3))
                .andExpect(jsonPath("$.selectedAnswers[0]").value(1))
                .andExpect(jsonPath("$.flagged").value(false));
    }

    /*
     * 5. Trả về 404 khi question index không tồn tại
     * - When: Gửi request với index không có trong test
     * - Then: Trả về 404 Not Found
     */
    @Test
    @DisplayName("Returns 404 when question index not found")
    void testGetQuestion_NotFound() throws Exception {
        mockMvc.perform(get("/semester/test/result/" + studentResult.getId() + "/question/99")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isNotFound());
    }

    /*
     * 6. Student không thể xem câu hỏi của result khác
     * - When: Student gửi request với result của người khác
     * - Then: Trả về 403 Forbidden
     */
    @Test
    @DisplayName("Student cannot access question of another student's result")
    void testGetQuestion_CannotAccessOther() throws Exception {
        Long otherStudentId = 999L;

        mockMvc.perform(get("/semester/test/result/" + studentResult.getId() + "/question/0")
                .header("X-User-Id", otherStudentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());
    }

    /*
     * ===============================================
     * TEST: PUT /semester/test/result/{resultId}/question/{questionIndex}/answer
     * ===============================================
     */

    /*
     * 7. Student có thể chọn đáp án đơn
     * - When: Student gửi request chọn 1 đáp án
     * - Then: Cập nhật đáp án đã chọn, status 200
     */
    @Test
    @DisplayName("Student can select single answer")
    void testSelectAnswer_SingleAnswer() throws Exception {
        SelectAnswerRequest request = new SelectAnswerRequest();
        request.setAnswerIndices(Arrays.asList(2));

        mockMvc.perform(put("/semester/test/result/" + studentResult.getId() + "/question/1/answer")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Answer updated successfully"));

        // Verify in database
        Result updated = resultRepository.findById(studentResult.getId()).get();
        JsonNode studentAnswers = updated.getStudentAnswers();
        JsonNode answer1 = studentAnswers.get("1");
        assert answer1.get("selectedAnswers").get(0).asInt() == 2;
    }

    /*
     * 8. Student có thể chọn nhiều đáp án
     * - When: Student gửi request chọn nhiều đáp án
     * - Then: Cập nhật tất cả đáp án đã chọn, status 200
     */
    @Test
    @DisplayName("Student can select multiple answers")
    void testSelectAnswer_MultipleAnswers() throws Exception {
        SelectAnswerRequest request = new SelectAnswerRequest();
        request.setAnswerIndices(Arrays.asList(0, 2));

        mockMvc.perform(put("/semester/test/result/" + studentResult.getId() + "/question/1/answer")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify in database
        Result updated = resultRepository.findById(studentResult.getId()).get();
        ArrayNode selectedAnswers = (ArrayNode) updated.getStudentAnswers().get("1").get("selectedAnswers");
        assert selectedAnswers.size() == 2;
        assert selectedAnswers.get(0).asInt() == 0;
        assert selectedAnswers.get(1).asInt() == 2;
    }

    /*
     * 9. Student có thể xóa đáp án đã chọn
     * - When: Student gửi request với array rỗng
     * - Then: Xóa tất cả đáp án đã chọn, status 200
     */
    @Test
    @DisplayName("Student can clear answer selection")
    void testSelectAnswer_ClearAnswer() throws Exception {
        SelectAnswerRequest request = new SelectAnswerRequest();
        request.setAnswerIndices(new ArrayList<>());

        mockMvc.perform(put("/semester/test/result/" + studentResult.getId() + "/question/0/answer")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify in database
        Result updated = resultRepository.findById(studentResult.getId()).get();
        ArrayNode selectedAnswers = (ArrayNode) updated.getStudentAnswers().get("0").get("selectedAnswers");
        assert selectedAnswers.size() == 0;
    }

    /*
     * 10. Không thể chọn đáp án sau khi submit
     * - Given: Result đã được submit
     * - When: Student gửi request chọn đáp án
     * - Then: Trả về 400 Bad Request
     */
    @Test
    @DisplayName("Cannot select answer after test submitted")
    void testSelectAnswer_AfterSubmit() throws Exception {
        studentResult.setSubmitDateTime(LocalDateTime.now());
        resultRepository.save(studentResult);

        SelectAnswerRequest request = new SelectAnswerRequest();
        request.setAnswerIndices(Arrays.asList(1));

        mockMvc.perform(put("/semester/test/result/" + studentResult.getId() + "/question/0/answer")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /*
     * 11. Không thể chọn đáp án trên result của người khác
     * - When: Student gửi request với result của người khác
     * - Then: Trả về 403 Forbidden
     */
    @Test
    @DisplayName("Cannot select answer on another student's result")
    void testSelectAnswer_CannotAccessOther() throws Exception {
        SelectAnswerRequest request = new SelectAnswerRequest();
        request.setAnswerIndices(Arrays.asList(1));

        Long otherStudentId = 999L;

        mockMvc.perform(put("/semester/test/result/" + studentResult.getId() + "/question/0/answer")
                .header("X-User-Id", otherStudentId.toString())
                .header("X-User-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /*
     * 12. Trả về 400 khi answerIndices null
     * - When: Gửi request không có answerIndices
     * - Then: Trả về 400 Bad Request
     */
    @Test
    @DisplayName("Returns 400 when answerIndices is null")
    void testSelectAnswer_NullAnswerIndices() throws Exception {
        String invalidRequest = "{}";

        mockMvc.perform(put("/semester/test/result/" + studentResult.getId() + "/question/0/answer")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    /*
     * ===============================================
     * TEST: PUT /semester/test/result/{resultId}/question/{questionIndex}/flag
     * ===============================================
     */

    /*
     * 13. Student có thể đánh dấu câu hỏi
     * - When: Student gửi request flag câu hỏi
     * - Then: Cập nhật flag = true, status 200
     */
    @Test
    @DisplayName("Student can flag a question")
    void testFlagQuestion_Flag() throws Exception {
        mockMvc.perform(put("/semester/test/result/" + studentResult.getId() + "/question/0/flag")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT")
                .param("flagged", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Question flag updated successfully"));

        // Verify in database
        Result updated = resultRepository.findById(studentResult.getId()).get();
        boolean flagged = updated.getStudentAnswers().get("0").get("flagged").asBoolean();
        assert flagged == true;
    }

    /*
     * 14. Student có thể bỏ đánh dấu câu hỏi
     * - Given: Câu hỏi đã được flag
     * - When: Student gửi request unflag
     * - Then: Cập nhật flag = false, status 200
     */
    @Test
    @DisplayName("Student can unflag a question")
    void testFlagQuestion_Unflag() throws Exception {
        mockMvc.perform(put("/semester/test/result/" + studentResult.getId() + "/question/1/flag")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT")
                .param("flagged", "false"))
                .andExpect(status().isOk());

        // Verify in database
        Result updated = resultRepository.findById(studentResult.getId()).get();
        boolean flagged = updated.getStudentAnswers().get("1").get("flagged").asBoolean();
        assert flagged == false;
    }

    /*
     * 15. Không thể flag câu hỏi sau khi submit
     * - Given: Result đã được submit
     * - When: Student gửi request flag
     * - Then: Trả về 400 Bad Request
     */
    @Test
    @DisplayName("Cannot flag question after test submitted")
    void testFlagQuestion_AfterSubmit() throws Exception {
        studentResult.setSubmitDateTime(LocalDateTime.now());
        resultRepository.save(studentResult);

        mockMvc.perform(put("/semester/test/result/" + studentResult.getId() + "/question/0/flag")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT")
                .param("flagged", "true"))
                .andExpect(status().isBadRequest());
    }

    /*
     * ===============================================
     * TEST: GET /semester/test/{semesterTestId}/my-results
     * ===============================================
     */

    /*
     * 16. Student có thể lấy danh sách kết quả của mình
     * - Given: Student có 2 result (1 submitted, 1 in-progress)
     * - When: Student gửi request
     * - Then: Trả về cả 2 result, status 200
     */
    @Test
    @DisplayName("Student can get their results")
    void testGetMyResults_StudentSuccess() throws Exception {
        // Create second result
        Result result2 = new Result();
        result2.setSemesterTest(semesterTest);
        result2.setStudentId(studentId);
        result2.setStartDateTime(LocalDateTime.now().minusMinutes(10));
        result2.setSubmitDateTime(null);
        resultRepository.save(result2);

        // Submit first result
        studentResult.setSubmitDateTime(LocalDateTime.now());
        studentResult.setScore(8.0f);
        resultRepository.save(studentResult);

        mockMvc.perform(get("/semester/test/" + semesterTest.getId() + "/my-results")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].studentId").value(studentId))
                .andExpect(jsonPath("$[1].id").exists());
    }

    /*
     * 17. Trả về danh sách rỗng khi chưa có result
     * - When: Student gửi request nhưng chưa từng làm bài
     * - Then: Trả về array rỗng, status 200
     */
    @Test
    @DisplayName("Returns empty list when no results")
    void testGetMyResults_EmptyList() throws Exception {
        Long newStudentId = 999L;
        createSemesterAccountForStudent(newStudentId);

        mockMvc.perform(get("/semester/test/" + semesterTest.getId() + "/my-results")
                .header("X-User-Id", newStudentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /*
     * ===============================================
     * TEST: GET /semester/test/result/{resultId}
     * ===============================================
     */

    /*
     * 18. Student có thể xem chi tiết result của mình (chưa submit - không có
     * trueAnswers)
     * - Given: Student có result chưa submit
     * - When: Student gửi request xem detail
     * - Then: Trả về thông tin result nhưng không có trueAnswers, status 200
     */
    @Test
    @DisplayName("Student can get their result detail without trueAnswers before submit")
    void testGetResultDetail_BeforeSubmit() throws Exception {
        mockMvc.perform(get("/semester/test/result/" + studentResult.getId())
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentResult.getId()))
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.detailTest").exists())
                .andExpect(jsonPath("$.studentAnswers").exists())
                .andExpect(jsonPath("$.trueAnswers").doesNotExist())
                .andExpect(jsonPath("$.minutes").value(60));
    }

    /*
     * 19. Student có thể xem chi tiết result sau khi submit (có trueAnswers)
     * - Given: Student đã submit result
     * - When: Student gửi request xem detail
     * - Then: Trả về thông tin result bao gồm trueAnswers, status 200
     */
    @Test
    @DisplayName("Student can get their result detail with trueAnswers after submit")
    void testGetResultDetail_AfterSubmit() throws Exception {
        studentResult.setSubmitDateTime(LocalDateTime.now());
        studentResult.setScore(8.0f);
        resultRepository.save(studentResult);

        mockMvc.perform(get("/semester/test/result/" + studentResult.getId())
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentResult.getId()))
                .andExpect(jsonPath("$.score").value(8.0))
                .andExpect(jsonPath("$.trueAnswers").exists());
    }

    /*
     * 20. Admin có thể xem chi tiết bất kỳ result nào (luôn có trueAnswers)
     * - When: Admin gửi request xem detail
     * - Then: Trả về thông tin result bao gồm trueAnswers, status 200
     */
    @Test
    @DisplayName("Admin can get any result detail with trueAnswers")
    void testGetResultDetail_AdminSuccess() throws Exception {
        mockMvc.perform(get("/semester/test/result/" + studentResult.getId())
                .header("X-User-Id", adminId.toString())
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentResult.getId()))
                .andExpect(jsonPath("$.trueAnswers").exists());
    }

    /*
     * 21. Student không thể xem result của người khác
     * - When: Student gửi request xem result của người khác
     * - Then: Trả về 403 Forbidden
     */
    @Test
    @DisplayName("Student cannot access another student's result")
    void testGetResultDetail_CannotAccessOther() throws Exception {
        Long otherStudentId = 999L;

        mockMvc.perform(get("/semester/test/result/" + studentResult.getId())
                .header("X-User-Id", otherStudentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());
    }

    /*
     * 22. Trả về 404 khi result không tồn tại
     * - When: Gửi request với resultId không tồn tại
     * - Then: Trả về 404 Not Found
     */
    @Test
    @DisplayName("Returns 404 when result not found")
    void testGetResultDetail_NotFound() throws Exception {
        mockMvc.perform(get("/semester/test/result/99999")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isNotFound());
    }

    /*
     * ===============================================
     * TEST: GET /{semesterId}/exams
     * ===============================================
     */

    /*
     * 23. Có thể lấy tất cả bài thi EXAM trong semester
     * - Given: Semester có 2 EXAM test
     * - When: Gửi request
     * - Then: Trả về danh sách 2 EXAM, status 200
     */
    @Test
    @DisplayName("Can get all exam tests in semester")
    void testGetTestExams_Success() throws Exception {
        // Create second EXAM test
        SemesterTest exam2 = new SemesterTest();
        exam2.setName("Final Exam");
        exam2.setSemester(semester);
        exam2.setTest(test);
        exam2.setType(TestType.EXAM);
        exam2.setMinutes(90);
        exam2.setStartDate(LocalDateTime.now().plusDays(7));
        exam2.setEndDate(LocalDateTime.now().plusDays(7).plusHours(2));
        exam2.setOpen(false);
        exam2.setCreatedBy(adminId);
        semesterTestRepository.save(exam2);

        mockMvc.perform(get("/semester/" + semester.getId() + "/exams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("EXAM"))
                .andExpect(jsonPath("$[1].type").value("EXAM"));
    }

    /*
     * 24. Trả về danh sách rỗng khi không có EXAM
     * - Given: Semester không có EXAM test (chỉ có test khác)
     * - When: Gửi request
     * - Then: Trả về array rỗng, status 200
     */
    @Test
    @DisplayName("Returns empty list when no exams in semester")
    void testGetTestExams_EmptyList() throws Exception {
        // Change semesterTest to PRACTICE
        semesterTest.setType(TestType.PRACTICE);
        semesterTestRepository.save(semesterTest);

        mockMvc.perform(get("/semester/" + semester.getId() + "/exams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /*
     * 25. Endpoint không yêu cầu authentication
     * - When: Gửi request không có header authentication
     * - Then: Vẫn trả về kết quả, status 200
     */
    @Test
    @DisplayName("Can access exams endpoint without authentication")
    void testGetTestExams_NoAuth() throws Exception {
        mockMvc.perform(get("/semester/" + semester.getId() + "/exams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}