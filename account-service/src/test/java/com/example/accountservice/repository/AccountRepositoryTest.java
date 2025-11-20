package com.example.accountservice.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AccountRepositoryTest {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Account studentAccount;
    private Account teacherAccount;
    private Account adminAccount;

    /*
     * Hàm xử lý trước mỗi test case:
     * - Xóa dữ liệu cũ trong db
     * - Tạo và lưu 3 account test (student, teacher, admin)
     */
    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();

        studentAccount = createAccount("student", "Student", "User", Role.STUDENT, "111111111");
        teacherAccount = createAccount("teacher", "Teacher", "User", Role.TEACHER, "222222222");
        adminAccount = createAccount("admin", "Admin", "User", Role.ADMIN, "333333333");

        studentAccount = accountRepository.save(studentAccount);
        teacherAccount = accountRepository.save(teacherAccount);
        adminAccount = accountRepository.save(adminAccount);
    }

    /*
     * Danh sách các test case:
     * 1. Tìm tài khoản theo username và visible = 1 (Thành công)
     * 2. Tìm tài khoản theo username và visible = 1 (Không tìm thấy vì visible = 0)
     * 3. Tìm tài khoản theo username và visible = 1 (Không tìm thấy vì không tồn tại Account có username này)
     * 4. Tìm tài khoản theo id và visible = 1 (Thành công)
     * 5. Tìm tài khoản theo id và visible = 1 (Không tìm thấy vì visible = 0)
     * 6. Tìm tài khoản theo id và visible = 1 (Không tìm thấy vì không tồn tại Account có id này)
     * 7. Kiểm tra sự tồn tại của tài khoản theo CCCD và visible = 1 (Thành công)
     * 8. Kiểm tra sự tồn tại của tài khoản theo CCCD và visible = 1 (Không tìm thấy vì visible = 0)
     * 9. Kiểm tra sự tồn tại của tài khoản theo CCCD và visible = 1 (Không tìm thấy vì không tồn tại Account có CCCD này)
     * 10. Kiểm tra sự tồn tại của tài khoản theo email và visible = 1 (Thành công)
     * 11. Kiểm tra sự tồn tại của tài khoản theo email và visible = 1 (Không tìm thấy vì visible = 0)
     * 12. Kiểm tra sự tồn tại của tài khoản theo email và visible = 1 (Không tìm thấy vì không tồn tại Account có email này)
     * 13. Tìm tài khoản theo CCCD và visible = 1 (Thành công)
     * 14. Tìm tài khoản theo CCCD và visible = 1 (Không tìm thấy vì visible = 0)
     * 15. Tìm tài khoản theo CCCD và visible = 1 (Không tìm thấy vì không tồn tại Account có CCCD này)
     * 16. Tìm tài khoản theo email và visible = 1 (Thành công)
     * 17. Tìm tài khoản theo email và visible = 1 (Không tìm thấy vì visible = 0)
     * 18. Tìm tài khoản theo email và visible = 1 (Không tìm thấy vì không tồn tại Account có email này)
     */

    /*
     * 1. Tìm tài khoản theo username và visible = 1 (Thành công)
     * - When: Gọi hàm tìm kiếm username là student và visible = 1
     * - Then: Tìm thấy tài khoản với các thông tin khớp với tài khoản student
     */
    @Test
    void testFindByUsernameAndVisible_Success() {
        Optional<Account> accountOpt = accountRepository.findByUsernameAndVisible("student", 1);

        assert(accountOpt.isPresent());
        assert(accountOpt.get().getUsername().equals("student"));
        assert(accountOpt.get().getRole().equals(Role.STUDENT));
        assert(accountOpt.get().getEmail().equals("student@example.com"));
    }

    /*
     * 2. Tìm tài khoản theo username và visible = 1 (Không tìm thấy vì visible = 0)
     * - Given: Tài khoản student có visible = 0
     * - When: GỌi hàm tìm kiếm username là student và visible = 1
     * - Then: Không tìm thấy tài khoản
     */
    @Test
    void testFindByUsernameAndVisible_NotFound() {
        studentAccount.setVisible(0);
        accountRepository.save(studentAccount);
        
        Optional<Account> accountOpt = accountRepository.findByUsernameAndVisible("student", 1);
        assertTrue(accountOpt.isEmpty(), "Account không tìm thấy vì visible = 0"); 
    }

    /*
     * 3. Tìm tài khoản theo username và visible = 1 (Không tìm thấy vì không tồn tại Account có username này)
     * - When: GỌi hàm tìm kiếm username là nonexistent và visible = 1
     * - Then: Không tìm thấy tài khoản
     */
    @Test 
    void testFindByUsernameAndVisible_NotExist() {
        Optional<Account> accountOpt = accountRepository.findByUsernameAndVisible("nonexistent", 1);
        assertTrue(accountOpt.isEmpty(), "Account không tồn tại");
    }

    /*
     * 4. Tìm tài khoản theo id và visible = 1 (Thành công)
     * - When: Gọi hàm tìm kiếm theo id của tài khoản teacher và visible = 1
     * - Then: Tìm thấy tài khoản với các thông tin khớp với tài khoản teacher
     */
    @Test
    void testFindByIdAndVisible_Success() {
        Optional<Account> accountOpt = accountRepository.findByIdAndVisible(teacherAccount.getId(), 1);

        assert(accountOpt.isPresent());
        assert(accountOpt.get().getUsername().equals("teacher"));
        assert(accountOpt.get().getRole().equals(Role.TEACHER));
        assert(accountOpt.get().getEmail().equals("teacher@example.com"));
    }

    /*
     * 5. Tìm tài khoản theo id và visible = 1 (Không tìm thấy vì visible = 0)
     * - Given: Tài khoản teacher có visible = 0
     * - When: GỌi hàm tìm kiếm theo id của tài khoản teacher và visible = 1
     * - Then: Không tìm thấy tài khoản
     */
    @Test
    void testFindByIdAndVisible_NotFound() {
        teacherAccount.setVisible(0);
        accountRepository.save(teacherAccount);
        
        Optional<Account> accountOpt = accountRepository.findByIdAndVisible(teacherAccount.getId(), 1);
        assertTrue(accountOpt.isEmpty(), "Account không tìm thấy vì visible = 0");
    }

    /*
     * 6. Tìm tài khoản theo id và visible = 1 (Không tìm thấy vì không tồn tại Account có id này)
     * - When: Gọi hàm tìm kiếm theo id không tồn tại và visible = 1
     * - Then: Không tìm thấy tài khoản
     */
    @Test
    void testFindByIdAndVisible_NotExist() {
        Optional<Account> accountOpt = accountRepository.findByIdAndVisible(999L, 1);
        assertTrue(accountOpt.isEmpty(), "Account không tồn tại");
    }

    /*
     * 7. Kiểm tra sự tồn tại của tài khoản theo CCCD và visible = 1 (Thành công)
     * - When: Gọi hàm kiểm tra sự tồn tại của tài khoản với cccd của tài khoản student và visible = 1
     * - Then: Kết quả trả về true
     */
    @Test
    void testExistByCccdAndVisible_Success() {
        boolean exists = accountRepository.existsByCccdAndVisible("111111111", 1);
        assertTrue(exists, "Account với cccd 111111111 và visible = 1 tồn tại");   
    }

    /*
     * 8. Kiểm tra sự tồn tại của tài khoản theo CCCD và visible = 1 (Không tìm thấy vì visible = 0)
     * - Given: Tài khoản student có visible = 0
     * - When: Gọi hàm kiểm tra sự tồn tại của tài khoản với cccd của tài khoản student và visible = 1
     * - Then: Kết quả trả về false
     */
    @Test
    void testExistByCccdAndVisible_NotFound() {
        studentAccount.setVisible(0);
        accountRepository.save(studentAccount);

        boolean exists = accountRepository.existsByCccdAndVisible("111111111", 1);
        assertFalse(exists, "Account với cccd 111111111 không tồn tại vì visible = 0");
    }

    /*
     * 9. Kiểm tra sự tồn tại của tài khoản theo CCCD và visible = 1 (Không tìm thấy vì không tồn tại Account có CCCD này)
     * - When: Gọi hàm kiểm tra sự tồn tại của tài khoản với cccd không tồn tại và visible = 1
     * - Then: Kết quả trả về false
     */
    @Test
    void testExistByCccdAndVisible_NotExist() {
        boolean exists = accountRepository.existsByCccdAndVisible("999999999", 1);
        assertFalse(exists, "Account với cccd 999999999 không tồn tại");
    }

    /*
     * 10. Kiểm tra sự tồn tại của tài khoản theo email và visible = 1 (Thành công)
     * - When: Gọi hàm kiểm tra sự tồn tại của tài khoản với email của tài khoản student và visible = 1
     * - Then: Kết quả trả về true
     */
    @Test
    void testExistByEmailAndVisible_Success() {
        boolean exists = accountRepository.existsByEmailAndVisible(studentAccount.getEmail(), 1);
        assertTrue(exists, "Account : " + studentAccount.getEmail() + " visible = 1");
    }

    /*
     * 11. Kiểm tra sự tồn tại của tài khoản theo email và visible = 1 (Không tìm thấy vì visible = 0)
     * - Given: Tài khoản student có visible = 0
     * - When: Gọi hàm kiểm tra sự tồn tại của tài khoản với email của tài khoản student và visible = 1
     * - Then: Kết quả trả về false
     */
    @Test
    void testExistByEmailAndVisible_NotFound() {
        studentAccount.setVisible(0);
        accountRepository.save(studentAccount);
        
        boolean exists = accountRepository.existsByEmailAndVisible(studentAccount.getEmail(), 1);
        assertFalse(exists, "Account : " + studentAccount.getEmail() + " không tìm thấy vì visible = 0");
    }

    /*
     * 12. Kiểm tra sự tồn tại của tài khoản theo email và visible = 1 (Không tìm thấy vì không tồn tại Account có email này)
     * - When: Gọi hàm kiểm tra sự tồn tại của tài khoản với email không tồn tại và visible = 1
     * - Then: Kết quả trả về false
     */
    @Test
    void testExistByEmailAndVisible_NotExist() {
        boolean exists = accountRepository.existsByEmailAndVisible("notexist@example.com", 1);
        assertFalse(exists, "Email : notexist@example.com không tồn tại");
    }

    /*
     * 13. Tìm tài khoản theo CCCD và visible = 1 (Thành công)
     * - When: Gọi hàm tìm tất cả tài khoản với cccd của tài khoản student và visible = 1
     * - Then: Tìm thấy tài khoản với các thông tin khớp với tài khoản student
     */
    @Test
    void testFindByCccdAndVisible_Success() {
        Optional<Account> accountOpt = accountRepository.findByCccdAndVisible(studentAccount.getCccd(), 1);
        assert(accountOpt.isPresent());
        assert(accountOpt.get().getUsername().equals("student"));
        assert(accountOpt.get().getRole().equals(Role.STUDENT));
    }

    /*
     * 14. Tìm tài khoản theo CCCD và visible = 1 (Không tìm thấy vì visible = 0)
     * - Given: Tài khoản student có visible = 0
     * - When: Gọi hàm tìm tất cả tài khoản với cccd của tài khoản student và visible = 1
     * - Then: Không tìm thấy tài khoản
     */
    @Test
    void testFindByCccdAndVisible_NotFound() {
        studentAccount.setVisible(0);
        accountRepository.save(studentAccount);
        
        Optional<Account> accountOpt = accountRepository.findByCccdAndVisible(studentAccount.getCccd(), 1);
        assertTrue(accountOpt.isEmpty(), "Account không tìm thấy vì visible = 0");
    }

    /*
     * 15. Tìm tài khoản theo CCCD và visible = 1 (Không tìm thấy vì không tồn tại Account có CCCD này)
     * - When: Gọi hàm tìm tất cả tài khoản với cccd không tồn tại và visible = 1
     * - Then: Không tìm thấy tài khoản
     */
    @Test
    void testFindByCccdAndVisible_NotExist() {
        Optional<Account> accountOpt = accountRepository.findByCccdAndVisible("9999999999", 1);
        assertTrue(accountOpt.isEmpty(), "Account không tồn tại");
    }

    /*
     * 16. Tìm tài khoản theo email và visible = 1 (Thành công)
     * - When: Gọi hàm tìm tất cả tài khoản với email của tài khoản student và visible = 1
     * - Then: Tìm thấy tài khoản với các thông tin khớp với tài khoản student
     */
    @Test
    void testFindByEmailAndVisible_Success() {
        Optional<Account> accountOpt = accountRepository.findByEmailAndVisible(studentAccount.getEmail(), 1);
        assert(accountOpt.isPresent());
        assert(accountOpt.get().getUsername().equals("student"));
        assert(accountOpt.get().getRole().equals(Role.STUDENT));
    }

    /*
     * 17. Tìm tài khoản theo email và visible = 1 (Không tìm thấy vì visible = 0)
     * - Given: Tài khoản student có visible = 0
     * - When: Gọi hàm tìm tất cả tài khoản với email của tài khoản student và visible = 1
     * - Then: Không tìm thấy tài khoản
     */
    @Test
    void testFindByEmailAndVisible_NotFound() {
        studentAccount.setVisible(0);
        accountRepository.save(studentAccount);
        
        Optional<Account> accountOpt = accountRepository.findByEmailAndVisible(studentAccount.getEmail(), 1);
        assertTrue(accountOpt.isEmpty(), "Account không tìm thấy vì visible = 0");
    }

    /*
     * 18. Tìm tài khoản theo email và visible = 1 (Không tìm thấy vì không tồn tại Account có email này)
     * - When: Gọi hàm tìm tất cả tài khoản với email không tồn tại và visible = 1
     * - Then: Không tìm thấy tài khoản
     */
    @Test
    void testFindByEmailAndVisible_NotExist() {
        Optional<Account> accountOpt = accountRepository.findByEmailAndVisible("notexist@example.com", 1);
        assertTrue(accountOpt.isEmpty(), "Account không tồn tại");
    }


    /*
     * Hàm tiện ích tạo Account test nhanh
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
