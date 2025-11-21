package com.example.learnservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Integration test cho SemesterTestController với H2 database
 * Test các API cơ bản: getSemesterTestById, openTest, startTest, endTest,
 * getTestStatus
 */
@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
@Transactional
class SemesterTestControllerBasicDbTest {

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
    private Long studentId = 100L;
    private Long teacherId = 200L;
    private Long adminId = 1L;

    /*
     * Hàm xử lý trước mỗi test case:
     * - Thiết lập MockMvc
     * - Xóa dữ liệu cũ trong database
     * - Tạo và lưu dữ liệu test cần thiết
     */
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
        semesterTest.setOpen(false);
        semesterTest.setCreatedBy(adminId);
        semesterTest = semesterTestRepository.save(semesterTest);

        // Create SemesterAccount for student
        SemesterAccount semesterAccount = new SemesterAccount();
        semesterAccount.setSemester(semester);
        semesterAccount.setAccountId(studentId);
        semesterAccount.setPosition(position);
        semesterAccount.setCreatedBy(adminId);
        semesterAccountRepository.save(semesterAccount);
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

    /*
     * ===============================================
     * TEST: GET /semester/test/{semesterTestId}
     * ===============================================
     */

    /*
     * 1. Admin có thể lấy thông tin bất kỳ semester test nào
     * - Given: Có semester test trong database
     * - When: Admin gửi request lấy thông tin semester test
     * - Then: Trả về thông tin semester test, status 200
     */
    @Test
    @DisplayName("Admin can get any semester test")
    void testGetSemesterTestById_AdminSuccess() throws Exception {
        mockMvc.perform(get("/semester/test/" + semesterTest.getId())
                .header("X-User-Id", adminId.toString())
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(semesterTest.getId()))
                .andExpect(jsonPath("$.name").value("Midterm Exam"))
                .andExpect(jsonPath("$.minutes").value(60))
                .andExpect(jsonPath("$.type").value("EXAM"));
    }

    /*
     * 2. Student có thể lấy thông tin semester test nếu được enroll
     * - Given: Student được enroll vào semester
     * - When: Student gửi request lấy thông tin semester test
     * - Then: Trả về thông tin semester test, status 200
     */
    @Test
    @DisplayName("Student can get semester test if enrolled")
    void testGetSemesterTestById_StudentEnrolledSuccess() throws Exception {
        mockMvc.perform(get("/semester/test/" + semesterTest.getId())
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(semesterTest.getId()))
                .andExpect(jsonPath("$.name").value("Midterm Exam"));
    }

    /*
     * 3. Student không thể truy cập semester test nếu không được enroll
     * - Given: Student không được enroll vào semester
     * - When: Student gửi request lấy thông tin semester test
     * - Then: Trả về 403 Forbidden
     */
    @Test
    @DisplayName("Student cannot access semester test if not enrolled")
    void testGetSemesterTestById_StudentNotEnrolled() throws Exception {
        Long otherStudentId = 999L;

        mockMvc.perform(get("/semester/test/" + semesterTest.getId())
                .header("X-User-Id", otherStudentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());
    }

    /*
     * 4. Trả về 404 khi semester test không tồn tại
     * - When: Gửi request với id không tồn tại
     * - Then: Trả về 404 Not Found
     */
    @Test
    @DisplayName("Returns 404 when semester test not found")
    void testGetSemesterTestById_NotFound() throws Exception {
        mockMvc.perform(get("/semester/test/99999")
                .header("X-User-Id", adminId.toString())
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNotFound());
    }

    /*
     * ===============================================
     * TEST: POST /semester/test/{semesterTestId}/open
     * ===============================================
     */

    /*
     * 5. Admin có thể mở bài thi
     * - Given: Bài thi đang đóng (open = false)
     * - When: Admin gửi request mở bài thi
     * - Then: Bài thi được mở (open = true), status 200
     */
    @Test
    @DisplayName("Admin can open test")
    void testOpenTest_AdminSuccess() throws Exception {
        mockMvc.perform(post("/semester/test/" + semesterTest.getId() + "/open")
                .header("X-User-Id", adminId.toString())
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Test opened successfully"));

        // Verify test is opened in database
        SemesterTest updatedTest = semesterTestRepository.findById(semesterTest.getId()).get();
        assert updatedTest.getOpen() == true;
    }

    /*
     * 6. Student không thể mở bài thi
     * - When: Student gửi request mở bài thi
     * - Then: Trả về 403 Forbidden
     */
    @Test
    @DisplayName("Student cannot open test")
    void testOpenTest_StudentForbidden() throws Exception {
        mockMvc.perform(post("/semester/test/" + semesterTest.getId() + "/open")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());

        // Verify test is still closed
        SemesterTest updatedTest = semesterTestRepository.findById(semesterTest.getId()).get();
        assert updatedTest.getOpen() == false;
    }

    /*
     * ===============================================
     * TEST: POST /semester/test/{semesterTestId}/start
     * ===============================================
     */

    /*
     * 7. Student có thể bắt đầu làm bài thi
     * - Given: Bài thi đã mở, trong thời gian làm bài
     * - When: Student gửi request bắt đầu làm bài
     * - Then: Tạo Result mới, trả về resultId, status 200
     */
    @Test
    @DisplayName("Student can start test successfully")
    void testStartTest_StudentSuccess() throws Exception {
        // Open the test first
        semesterTest.setOpen(true);
        semesterTestRepository.save(semesterTest);

        mockMvc.perform(post("/semester/test/" + semesterTest.getId() + "/start")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resultId").exists())
                .andExpect(jsonPath("$.message").value("Started test successfully"));

        // Verify result created in database
        List<Result> results = resultRepository.findBySemesterTestIdAndStudentId(semesterTest.getId(), studentId);
        assert results.size() == 1;
        assert results.get(0).getStudentId().equals(studentId);
        assert results.get(0).getStartDateTime() != null;
        assert results.get(0).getSubmitDateTime() == null;
    }

    /*
     * 8. Không thể bắt đầu làm bài nếu test chưa mở
     * - Given: Bài thi chưa mở (open = false)
     * - When: Student gửi request bắt đầu làm bài
     * - Then: Trả về 400 Bad Request
     */
    @Test
    @DisplayName("Cannot start test if not opened")
    void testStartTest_NotOpened() throws Exception {
        mockMvc.perform(post("/semester/test/" + semesterTest.getId() + "/start")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isBadRequest());
    }

    /*
     * 9. Không thể bắt đầu làm bài trước thời gian bắt đầu
     * - Given: Thời gian hiện tại < startDate
     * - When: Student gửi request bắt đầu làm bài
     * - Then: Trả về 400 Bad Request
     */
    @Test
    @DisplayName("Cannot start test before start time")
    void testStartTest_BeforeStartTime() throws Exception {
        semesterTest.setOpen(true);
        semesterTest.setStartDate(LocalDateTime.now().plusHours(1));
        semesterTestRepository.save(semesterTest);

        mockMvc.perform(post("/semester/test/" + semesterTest.getId() + "/start")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isBadRequest());
    }

    /*
     * 10. Không thể bắt đầu làm bài sau thời gian kết thúc
     * - Given: Thời gian hiện tại > endDate
     * - When: Student gửi request bắt đầu làm bài
     * - Then: Trả về 400 Bad Request
     */
    @Test
    @DisplayName("Cannot start test after end time")
    void testStartTest_AfterEndTime() throws Exception {
        semesterTest.setOpen(true);
        semesterTest.setStartDate(LocalDateTime.now().minusHours(3));
        semesterTest.setEndDate(LocalDateTime.now().minusHours(1));
        semesterTestRepository.save(semesterTest);

        mockMvc.perform(post("/semester/test/" + semesterTest.getId() + "/start")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isBadRequest());
    }

    /*
     * 11. Không thể làm lại EXAM nếu đã submit
     * - Given: Student đã submit EXAM test
     * - When: Student gửi request bắt đầu làm bài lại
     * - Then: Trả về 400 Bad Request
     */
    @Test
    @DisplayName("Cannot restart EXAM if already submitted")
    void testStartTest_ExamAlreadySubmitted() throws Exception {
        semesterTest.setOpen(true);
        semesterTestRepository.save(semesterTest);

        // Create a submitted result
        Result existingResult = new Result();
        existingResult.setSemesterTest(semesterTest);
        existingResult.setStudentId(studentId);
        existingResult.setStartDateTime(LocalDateTime.now().minusHours(1));
        existingResult.setSubmitDateTime(LocalDateTime.now().minusMinutes(30));
        existingResult.setScore(8.0f);
        resultRepository.save(existingResult);

        mockMvc.perform(post("/semester/test/" + semesterTest.getId() + "/start")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isBadRequest());
    }

    /*
     * 12. Có thể tiếp tục làm EXAM nếu chưa submit
     * - Given: Student đã start EXAM nhưng chưa submit
     * - When: Student gửi request start lại
     * - Then: Trả về result hiện tại, status 200
     */
    @Test
    @DisplayName("Can continue EXAM if not submitted")
    void testStartTest_ExamContinue() throws Exception {
        semesterTest.setOpen(true);
        semesterTestRepository.save(semesterTest);

        // Create an unsubmitted result
        Result existingResult = new Result();
        existingResult.setSemesterTest(semesterTest);
        existingResult.setStudentId(studentId);
        existingResult.setStartDateTime(LocalDateTime.now().minusMinutes(30));
        existingResult.setSubmitDateTime(null);
        existingResult = resultRepository.save(existingResult);

        mockMvc.perform(post("/semester/test/" + semesterTest.getId() + "/start")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resultId").value(existingResult.getId()));

        // Verify no new result created
        List<Result> results = resultRepository.findBySemesterTestIdAndStudentId(semesterTest.getId(), studentId);
        assert results.size() == 1;
    }

    /*
     * ===============================================
     * TEST: POST /semester/test/{resultId}/end
     * ===============================================
     */

    /*
     * 13. Student có thể kết thúc bài thi và nhận điểm
     * - Given: Student đã start test và trả lời câu hỏi
     * - When: Student gửi request kết thúc bài thi
     * - Then: Tính điểm và trả về, status 200
     */
    @Test
    @DisplayName("Student can end test and get score")
    void testEndTest_StudentSuccess() throws Exception {
        semesterTest.setOpen(true);
        semesterTestRepository.save(semesterTest);

        // Start test to create result
        Result result = new Result();
        result.setSemesterTest(semesterTest);
        result.setStudentId(studentId);
        result.setStartDateTime(LocalDateTime.now().minusMinutes(30));
        result.setDetailTest(objectMapper.createObjectNode());

        // Set student answers (answer question 0 correctly, question 1 incorrectly)
        ObjectNode studentAnswers = objectMapper.createObjectNode();
        ObjectNode answer0 = objectMapper.createObjectNode();
        answer0.set("selectedAnswers", objectMapper.createArrayNode().add(1));
        answer0.put("flagged", false);
        studentAnswers.set("0", answer0);

        ObjectNode answer1 = objectMapper.createObjectNode();
        answer1.set("selectedAnswers", objectMapper.createArrayNode().add(0));
        answer1.put("flagged", false);
        studentAnswers.set("1", answer1);

        result.setStudentAnswers(studentAnswers);

        // Set true answers
        ObjectNode trueAnswers = objectMapper.createObjectNode();
        trueAnswers.set("0", objectMapper.createArrayNode().add(1));
        trueAnswers.set("1", objectMapper.createArrayNode().add(1));
        result.setTrueAnswers(trueAnswers);

        result = resultRepository.save(result);

        mockMvc.perform(post("/semester/test/" + result.getId() + "/end")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resultId").value(result.getId()))
                .andExpect(jsonPath("$.score").value(1.0))
                .andExpect(jsonPath("$.message").value("Ended test successfully"));

        // Verify result updated in database
        Result updatedResult = resultRepository.findById(result.getId()).get();
        assert updatedResult.getSubmitDateTime() != null;
        assert updatedResult.getScore() == 1.0f;
    }

    /*
     * 14. Không thể kết thúc bài thi của người khác
     * - When: Student gửi request kết thúc bài thi của student khác
     * - Then: Trả về 403 Forbidden
     */
    @Test
    @DisplayName("Cannot end test that is not yours")
    void testEndTest_NotYours() throws Exception {
        semesterTest.setOpen(true);
        semesterTestRepository.save(semesterTest);

        Result result = new Result();
        result.setSemesterTest(semesterTest);
        result.setStudentId(studentId);
        result.setStartDateTime(LocalDateTime.now().minusMinutes(30));
        result = resultRepository.save(result);

        Long otherStudentId = 999L;

        mockMvc.perform(post("/semester/test/" + result.getId() + "/end")
                .header("X-User-Id", otherStudentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());
    }

    /*
     * 15. Điểm bằng 0 khi không có câu trả lời đúng
     * - Given: Student trả lời sai tất cả câu hỏi
     * - When: Student kết thúc bài thi
     * - Then: Trả về score = 0.0
     */
    @Test
    @DisplayName("Score is 0 when all answers are wrong")
    void testEndTest_ZeroScore() throws Exception {
        semesterTest.setOpen(true);
        semesterTestRepository.save(semesterTest);

        Result result = new Result();
        result.setSemesterTest(semesterTest);
        result.setStudentId(studentId);
        result.setStartDateTime(LocalDateTime.now().minusMinutes(30));

        // All wrong answers
        ObjectNode studentAnswersWrong = objectMapper.createObjectNode();
        ObjectNode wrongAnswer0 = objectMapper.createObjectNode();
        wrongAnswer0.set("selectedAnswers", objectMapper.createArrayNode().add(0));
        wrongAnswer0.put("flagged", false);
        studentAnswersWrong.set("0", wrongAnswer0);

        ObjectNode wrongAnswer1 = objectMapper.createObjectNode();
        wrongAnswer1.set("selectedAnswers", objectMapper.createArrayNode().add(0));
        wrongAnswer1.put("flagged", false);
        studentAnswersWrong.set("1", wrongAnswer1);

        result.setStudentAnswers(studentAnswersWrong);

        ObjectNode trueAnswersForWrong = objectMapper.createObjectNode();
        trueAnswersForWrong.set("0", objectMapper.createArrayNode().add(1));
        trueAnswersForWrong.set("1", objectMapper.createArrayNode().add(1));
        result.setTrueAnswers(trueAnswersForWrong);

        result = resultRepository.save(result);

        mockMvc.perform(post("/semester/test/" + result.getId() + "/end")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0.0));
    }

    /*
     * ===============================================
     * TEST: GET /semester/test/{semesterTestId}/status
     * ===============================================
     */

    /*
     * 16. Trả về NOT_STARTED khi chưa bắt đầu làm bài
     * - Given: Student chưa start test
     * - When: Gửi request check status
     * - Then: Trả về status = NOT_STARTED
     */
    @Test
    @DisplayName("Returns NOT_STARTED when student has not started test")
    void testGetTestStatus_NotStarted() throws Exception {
        mockMvc.perform(get("/semester/test/" + semesterTest.getId() + "/status")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semesterTestId").value(semesterTest.getId()))
                .andExpect(jsonPath("$.status").value("NOT_STARTED"));
    }

    /*
     * 17. Trả về IN_PROGRESS khi đang làm bài
     * - Given: Student đã start nhưng chưa submit
     * - When: Gửi request check status
     * - Then: Trả về status = IN_PROGRESS và resultId
     */
    @Test
    @DisplayName("Returns IN_PROGRESS when student is taking test")
    void testGetTestStatus_InProgress() throws Exception {
        Result result = new Result();
        result.setSemesterTest(semesterTest);
        result.setStudentId(studentId);
        result.setStartDateTime(LocalDateTime.now().minusMinutes(30));
        result.setSubmitDateTime(null);
        result = resultRepository.save(result);

        mockMvc.perform(get("/semester/test/" + semesterTest.getId() + "/status")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semesterTestId").value(semesterTest.getId()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.resultId").value(result.getId()));
    }

    /*
     * 18. Trả về COMPLETED khi đã submit
     * - Given: Student đã submit test
     * - When: Gửi request check status
     * - Then: Trả về status = COMPLETED, resultId và score
     */
    @Test
    @DisplayName("Returns COMPLETED when student has submitted test")
    void testGetTestStatus_Completed() throws Exception {
        Result result = new Result();
        result.setSemesterTest(semesterTest);
        result.setStudentId(studentId);
        result.setStartDateTime(LocalDateTime.now().minusHours(1));
        result.setSubmitDateTime(LocalDateTime.now().minusMinutes(30));
        result.setScore(8.5f);
        result = resultRepository.save(result);

        mockMvc.perform(get("/semester/test/" + semesterTest.getId() + "/status")
                .header("X-User-Id", studentId.toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semesterTestId").value(semesterTest.getId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.resultId").value(result.getId()))
                .andExpect(jsonPath("$.score").value(8.5));
    }
}