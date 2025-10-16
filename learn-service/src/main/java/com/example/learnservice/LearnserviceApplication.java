package com.example.learnservice;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.learnservice.dto.DocumentDTO;
import com.example.learnservice.model.Answer;
import com.example.learnservice.model.Position;
import com.example.learnservice.model.Question;
import com.example.learnservice.model.SemesterTest;
import com.example.learnservice.model.Test;
import com.example.learnservice.model.TestQuestion;
import com.example.learnservice.repository.AnswerRepository;
import com.example.learnservice.repository.PositionRepository;
import com.example.learnservice.repository.QuestionRepository;
import com.example.learnservice.repository.SemeterTestRepository;
import com.example.learnservice.repository.TestQuestionRepository;
import com.example.learnservice.repository.TestRepository;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
// @EnableScheduling
@EnableAsync
@Slf4j
public class LearnserviceApplication implements CommandLineRunner {

	@Autowired
	private KafkaTemplate<String, DocumentDTO> kafkaTemplate;

	@Autowired
	private PositionRepository positionRepository;

	@Autowired
	private TestRepository testRepository;

	@Autowired
	private QuestionRepository questionRepository;

	@Autowired
	private AnswerRepository answerRepository;

	@Autowired
	private TestQuestionRepository testQuestionRepository;

	@Autowired
	private SemeterTestRepository semesterTestRepository;

	public static void main(String[] args) {
		SpringApplication.run(LearnserviceApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// ✅ Warmup Kafka producer
		try {
			// Buộc producer connect tới broker mà không gửi message thật
			kafkaTemplate.partitionsFor("warmup-topic");
			log.info("Kafka producer warmup done ✅");
		} catch (Exception e) {
			log.warn("Kafka warmup failed: {}", e.getMessage());
		}

		// Init Position
		if (positionRepository.count() == 0) {
			Position position1 = new Position();
			position1.setName("Lớp trưởng");
			positionRepository.save(position1);
			Position position2 = new Position();
			position2.setName("Lớp phó");
			positionRepository.save(position2);
			Position position3 = new Position();
			position3.setName("Bí thư");
			positionRepository.save(position3);
			log.info("✅ Initialized 3 positions");
		}

		// Init Test data
		if (testRepository.count() == 0) {
			// Tạo Test
			Test test1 = new Test();
			test1.setName("Bài kiểm tra Java cơ bản");
			test1.setVisible(true);
			test1.setCreatedBy(1L);
			test1 = testRepository.save(test1);

			Test test2 = new Test();
			test2.setName("Bài kiểm tra Spring Boot");
			test2.setVisible(true);
			test2.setCreatedBy(1L);
			test2 = testRepository.save(test2);

			// Tạo Questions và Answers cho Test 1
			Question q1 = new Question();
			q1.setText("Java là ngôn ngữ lập trình gì?");
			q1.setCreatedBy(1L);
			q1 = questionRepository.save(q1);

			Answer a1_1 = new Answer();
			a1_1.setQuestion(q1);
			a1_1.setText("Ngôn ngữ hướng đối tượng");
			a1_1.setTrueAnswer(true);
			a1_1.setCreatedBy(1L);
			answerRepository.save(a1_1);

			Answer a1_2 = new Answer();
			a1_2.setQuestion(q1);
			a1_2.setText("Ngôn ngữ thủ tục");
			a1_2.setTrueAnswer(false);
			a1_2.setCreatedBy(1L);
			answerRepository.save(a1_2);

			Answer a1_3 = new Answer();
			a1_3.setQuestion(q1);
			a1_3.setText("Ngôn ngữ kịch bản");
			a1_3.setTrueAnswer(false);
			a1_3.setCreatedBy(1L);
			answerRepository.save(a1_3);

			Question q2 = new Question();
			q2.setText("JVM là viết tắt của gì?");
			q2.setCreatedBy(1L);
			q2 = questionRepository.save(q2);

			Answer a2_1 = new Answer();
			a2_1.setQuestion(q2);
			a2_1.setText("Java Virtual Machine");
			a2_1.setTrueAnswer(true);
			a2_1.setCreatedBy(1L);
			answerRepository.save(a2_1);

			Answer a2_2 = new Answer();
			a2_2.setQuestion(q2);
			a2_2.setText("Java Variable Machine");
			a2_2.setTrueAnswer(false);
			a2_2.setCreatedBy(1L);
			answerRepository.save(a2_2);

			Answer a2_3 = new Answer();
			a2_3.setQuestion(q2);
			a2_3.setText("Java Visual Manager");
			a2_3.setTrueAnswer(false);
			a2_3.setCreatedBy(1L);
			answerRepository.save(a2_3);

			Question q3 = new Question();
			q3.setText("Từ khóa nào dùng để kế thừa trong Java?");
			q3.setCreatedBy(1L);
			q3 = questionRepository.save(q3);

			Answer a3_1 = new Answer();
			a3_1.setQuestion(q3);
			a3_1.setText("extends");
			a3_1.setTrueAnswer(true);
			a3_1.setCreatedBy(1L);
			answerRepository.save(a3_1);

			Answer a3_2 = new Answer();
			a3_2.setQuestion(q3);
			a3_2.setText("implements");
			a3_2.setTrueAnswer(false);
			a3_2.setCreatedBy(1L);
			answerRepository.save(a3_2);

			Answer a3_3 = new Answer();
			a3_3.setQuestion(q3);
			a3_3.setText("inherit");
			a3_3.setTrueAnswer(false);
			a3_3.setCreatedBy(1L);
			answerRepository.save(a3_3);

			// Liên kết Questions với Test1
			TestQuestion tq1 = new TestQuestion();
			tq1.setTest(test1);
			tq1.setQuestion(q1);
			tq1.setCreatedBy(1L);
			testQuestionRepository.save(tq1);

			TestQuestion tq2 = new TestQuestion();
			tq2.setTest(test1);
			tq2.setQuestion(q2);
			tq2.setCreatedBy(1L);
			testQuestionRepository.save(tq2);

			TestQuestion tq3 = new TestQuestion();
			tq3.setTest(test1);
			tq3.setQuestion(q3);
			tq3.setCreatedBy(1L);
			testQuestionRepository.save(tq3);

			// Tạo Questions cho Test 2
			Question q4 = new Question();
			q4.setText("Spring Boot là gì?");
			q4.setCreatedBy(1L);
			q4 = questionRepository.save(q4);

			Answer a4_1 = new Answer();
			a4_1.setQuestion(q4);
			a4_1.setText("Framework Java để xây dựng ứng dụng");
			a4_1.setTrueAnswer(true);
			a4_1.setCreatedBy(1L);
			answerRepository.save(a4_1);

			Answer a4_2 = new Answer();
			a4_2.setQuestion(q4);
			a4_2.setText("Ngôn ngữ lập trình");
			a4_2.setTrueAnswer(false);
			a4_2.setCreatedBy(1L);
			answerRepository.save(a4_2);

			Question q5 = new Question();
			q5.setText("Annotation nào để đánh dấu một class là REST Controller?");
			q5.setCreatedBy(1L);
			q5 = questionRepository.save(q5);

			Answer a5_1 = new Answer();
			a5_1.setQuestion(q5);
			a5_1.setText("@RestController");
			a5_1.setTrueAnswer(true);
			a5_1.setCreatedBy(1L);
			answerRepository.save(a5_1);

			Answer a5_2 = new Answer();
			a5_2.setQuestion(q5);
			a5_2.setText("@Controller");
			a5_2.setTrueAnswer(false);
			a5_2.setCreatedBy(1L);
			answerRepository.save(a5_2);

			Answer a5_3 = new Answer();
			a5_3.setQuestion(q5);
			a5_3.setText("@Service");
			a5_3.setTrueAnswer(false);
			a5_3.setCreatedBy(1L);
			answerRepository.save(a5_3);

			TestQuestion tq4 = new TestQuestion();
			tq4.setTest(test2);
			tq4.setQuestion(q4);
			tq4.setCreatedBy(1L);
			testQuestionRepository.save(tq4);

			TestQuestion tq5 = new TestQuestion();
			tq5.setTest(test2);
			tq5.setQuestion(q5);
			tq5.setCreatedBy(1L);
			testQuestionRepository.save(tq5);

			// Tạo SemesterTest
			SemesterTest st1 = new SemesterTest();
			st1.setName("Kiểm tra giữa kỳ - Java");
			st1.setTest(test1);
			st1.setStartDate(LocalDateTime.now().minusDays(1));
			st1.setEndDate(LocalDateTime.now().plusDays(7));
			st1.setCreatedBy(1L);
			semesterTestRepository.save(st1);

			SemesterTest st2 = new SemesterTest();
			st2.setName("Kiểm tra cuối kỳ - Spring Boot");
			st2.setTest(test2);
			st2.setStartDate(LocalDateTime.now().minusHours(2));
			st2.setEndDate(LocalDateTime.now().plusDays(14));
			st2.setCreatedBy(1L);
			semesterTestRepository.save(st2);

			log.info("✅ Initialized test data: 2 tests, 5 questions, 11 answers, 2 semester tests");
		}
	}

}