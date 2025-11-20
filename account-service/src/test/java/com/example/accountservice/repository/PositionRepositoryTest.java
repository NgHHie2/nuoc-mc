package com.example.accountservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.accountservice.model.Position;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PositionRepositoryTest {
    @Autowired
    private PositionRepository positionRepository;

    @BeforeEach
    void setUp() {
        positionRepository.deleteAll();
        Position position = new Position();
        position.setName("Developer");
        positionRepository.save(position);
    }

    /*
     * Test kiểm tra hàm existsByName khi vị trí tồn tại
     */
    // @Test
    // void testExistsByName() {
    //     boolean exists = positionRepository.existsByName("Developer");
    //     assert exists;

    //     boolean notExists = positionRepository.existsByName("Manager");
    //     assert !notExists;
    // }
}
