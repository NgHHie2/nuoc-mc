package com.example.learnservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.learnservice.dto.DocumentDTO;
import com.example.learnservice.model.Position;
import com.example.learnservice.repository.PositionRepository;

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
		}
	}

}