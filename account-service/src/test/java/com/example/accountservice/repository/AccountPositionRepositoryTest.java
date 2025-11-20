package com.example.accountservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.AccountPosition;
import com.example.accountservice.model.Position;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AccountPositionRepositoryTest {
    @Autowired
    private AccountPositionRepository accountPositionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PositionRepository positionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Account studentAccount;
    private Account teacherAccount;
    private Account adminAccount;

    private Position developerPosition;
    private Position managerPosition;

    private AccountPosition accountPositionStudentDev;
    private AccountPosition accountPositionTeacherDev;
    private AccountPosition accountPositionTeacherMan;
    private AccountPosition accountPositionAdmin;

    /*
     * Hàm xử lý trước mỗi test case:
     * - Xóa dữ liệu cũ trong database
     * - Tạo và lưu 3 account test (student, teacher, admin)
     * - Tạo và lưu 2 position test (developer, manager)
     * - Tạo và lưu các liên kết tài khoản-position
     */
    @BeforeEach
    void setUp() {
        accountPositionRepository.deleteAll();
        accountRepository.deleteAll();
        positionRepository.deleteAll();

        studentAccount = createAccount("student", "Student", "User", Role.STUDENT, "111111111");
        teacherAccount = createAccount("teacher", "Teacher", "User", Role.TEACHER, "222222222");
        adminAccount = createAccount("admin", "Admin", "User", Role.ADMIN, "333333333");

        studentAccount = accountRepository.save(studentAccount);
        teacherAccount = accountRepository.save(teacherAccount);
        adminAccount = accountRepository.save(adminAccount);

        developerPosition = new Position();
        developerPosition.setName("Developer");
        managerPosition = new Position();
        managerPosition.setName("Manager");
        developerPosition = positionRepository.save(developerPosition);
        managerPosition = positionRepository.save(managerPosition);

        accountPositionStudentDev = new AccountPosition();
        accountPositionTeacherDev = new AccountPosition();
        accountPositionTeacherMan = new AccountPosition();
        accountPositionAdmin = new AccountPosition();
        accountPositionStudentDev.setAccount(studentAccount);
        accountPositionStudentDev.setPosition(developerPosition);
        accountPositionTeacherDev.setAccount(teacherAccount);
        accountPositionTeacherDev.setPosition(developerPosition);
        accountPositionTeacherMan.setAccount(teacherAccount);
        accountPositionTeacherMan.setPosition(managerPosition);
        accountPositionAdmin.setAccount(adminAccount);
        accountPositionAdmin.setPosition(managerPosition);

        accountPositionStudentDev = accountPositionRepository.save(accountPositionStudentDev);
        accountPositionTeacherDev = accountPositionRepository.save(accountPositionTeacherDev);
        accountPositionTeacherMan = accountPositionRepository.save(accountPositionTeacherMan);
        accountPositionAdmin = accountPositionRepository.save(accountPositionAdmin);

    }

    /*
     * Danh sach các test case:
     * 1. Tìm tất cả vị trí theo tài khoản
     * 2. Tìm tất cả tài khoản theo vị trí
     * 3. Kiểm tra sự tồn tại giữa liên kết tài khoản-vị trí (trường hợp có tồn tại)
     * 4. Kiểm tra sự tồn tại giữa liên kết tài khoản-vị trí (trường hợp không tồn tại)
     * 5. Xóa liên kết tài khoản-vị trí (trường hợp có tồn tại)
     * 6. Xóa liên kết tài khoản-vị trí (trường hợp không tồn tại)
     */

    /*
     * 1. Tìm tất cả vị trí theo tài khoản
     * - When: Gọi hàm tìm tất cả vị trí theo tài khoản teacher
     * - Then: Kết quả trả về đúng 2 vị trí (developer, manager)
     */
    @Test
    void testFindByAccount() {
        var positions = accountPositionRepository.findByAccount(teacherAccount);

        assert(positions.size() == 2);
    }

    /*
     * 2. Tìm tất cả tài khoản theo vị trí
     * - When: Gọi hàm tìm tất cả tài khoản theo vị trí developer
     * - Then: Kết quả trả về đúng 2 tài khoản (student, teacher)
     */
    @Test
    void testFindByPosition() {
        var accounts = accountPositionRepository.findByPosition(developerPosition);

        assert(accounts.size() == 2);
    }

    /*
     * 3. Kiểm tra sự tồn tại giữa liên kết tài khoản-vị trí (trường hợp có tồn tại)
     * - When: Gọi hàm kiểm tra sự tồn tại giữa tài khoản teacher và vị trí developer
     * - Then: Kết quả trả về true
     */
    @Test
    void testExistsByAccountAndPosition_Exists() {
        boolean exists = accountPositionRepository.existsByAccountAndPosition(teacherAccount, developerPosition);

        assertTrue(exists);
    }

    /*
     * 4. Kiểm tra sự tồn tại giữa liên kết tài khoản-vị trí (trường hợp không tồn tại)
     * - When: Gọi hàm kiểm tra sự tồn tại giữa tài khoản student và vị trí manager
     * - Then: Kết quả trả về false
     */
    @Test
    void testExistsByAccountAndPosition_NotExists() {
        boolean exists = accountPositionRepository.existsByAccountAndPosition(studentAccount, managerPosition);

        assertFalse(exists);
    }

    /*
     * 5. Xóa liên kết tài khoản-vị trí (trường hợp có tồn tại)
     * - When: Gọi hàm xóa liên kết giữa tài khoản teacher với vị trí developer
     * - Then: Kết quả trả về số bản ghi đã xóa > 0
     * - Expected: Liên kết giữa tài khoản teacher với vị trí developer không còn tồn tại
     */
    @Test
    void testDeleteByAccountAndPosition_Exists() {
        long deleted = accountPositionRepository.deleteByAccountAndPosition(teacherAccount, developerPosition);

        assertTrue(deleted > 0);
        boolean exists = accountPositionRepository.existsByAccountAndPosition(teacherAccount, developerPosition);
        assertFalse(exists);
    }

    /*
     * 6. Xóa liên kết tài khoản-vị trí (trường hợp không tồn tại)
     * - When: Gọi hàm xóa liên kết giữa tài khoản student với vị trí manager
     * - Then: Kết quả trả về số bản ghi đã xóa = 0
     */
    @Test
    void testDeleteByAccountAndPosition_NotExists() {
        long deleted = accountPositionRepository.deleteByAccountAndPosition(studentAccount, managerPosition);

        assertEquals(0, deleted);
    }

    /*
     * Hàm tiện ích để tạo Account nhanh 
     */
    private Account createAccount(String username, String firstName, String lastName, Role role, String cccd) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode("password123"));
        account.setFirstName(firstName);
        account.setLastName(lastName);
        account.setEmail(username + "@example.com");
        account.setCccd(cccd);
        account.setRole(role);
        account.setVisible(1);
        account.setBirthDay(LocalDateTime.of(1990, 1, 1, 0, 0));
        return account;
    }
}
