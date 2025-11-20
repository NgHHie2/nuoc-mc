package com.example.accountservice.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;

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
import com.example.accountservice.repository.AccountPositionRepository;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.repository.PositionRepository;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class AccountPositionServiceTest {
    @Autowired
    private AccountPositionService accountPositionService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PositionRepository positionRepository;
    @Autowired
    private AccountPositionRepository accountPositionRepository;

    private Account adminAccount;
    private Account studentAccount;
    private Account teacherAccount;

    private Position leader;
    private Position member;
    private Position mentor;

    private AccountPosition adminPositionLeader;
    private AccountPosition studentPositionMember;
    private AccountPosition teacherPositionMentor;

    /*
     * Hàm xử lý trước mỗi test case:
     * - Xóa dữ liệu cũ trong database
     * - Tạo và lưu 3 account test (student, teacher, admin)
     * - Tạo và lưu 3 position test (leader, member, mentor)
     * - Tạo và lưu các liên kết account-position
     */
    @BeforeEach
    void setUp() {
        adminAccount = createAccount("adminuser", "Admin", "User", Role.ADMIN, "123456789");
        studentAccount = createAccount("studentuser", "Student", "User", Role.STUDENT, "987654321");
        teacherAccount = createAccount("teacheruser", "Teacher", "User", Role.TEACHER, "456789123");
        
        adminAccount = accountRepository.save(adminAccount);
        studentAccount = accountRepository.save(studentAccount);
        teacherAccount = accountRepository.save(teacherAccount);

        leader = new Position();
        leader.setName("Leader");
        leader.setDescription("Team Leader");

        member = new Position();
        member.setName("Member");
        member.setDescription("Team Member");

        mentor = new Position();
        mentor.setName("Mentor");
        mentor.setDescription("Team Mentor");

        leader = positionRepository.save(leader);
        member = positionRepository.save(member);
        mentor = positionRepository.save(mentor);
        
        adminPositionLeader = new AccountPosition();
        adminPositionLeader.setAccount(adminAccount);
        adminPositionLeader.setPosition(leader);

        studentPositionMember = new AccountPosition();
        studentPositionMember.setAccount(studentAccount);
        studentPositionMember.setPosition(member);

        teacherPositionMentor = new AccountPosition();
        teacherPositionMentor.setAccount(teacherAccount);
        teacherPositionMentor.setPosition(mentor);

        adminPositionLeader = accountPositionRepository.save(adminPositionLeader);
        studentPositionMember = accountPositionRepository.save(studentPositionMember);
        teacherPositionMentor = accountPositionRepository.save(teacherPositionMentor);
    }

    /*
     * Danh sách các test case:
     * 1. Lấy vị trí theo tài khoản thành công
     * 2. Lấy vị trí của 1 tài khoản không tồn tại
     * 3. Lấy tài khoản theo vị trí thành công
     * 4. Lấy tài khoản theo 1 vị trí không tồn tại
     * 5. Gán vị trí cho 1 tài khoản thành công
     * 6. Gán vị trí cho 1 tài khoản không tồn tại
     * 7. Gán 1 vị trí không tồn tại cho tài khoản
     * 8. Gán 1 vị trí đã có cho tài khoản
     * 9. Xóa 1 vị trí của tài khoản thành công
     * 10. Xóa vị trí của 1 tài khoản không tồn tại
     * 11. Xóa 1 vị trí không tồn tại của tài khoản
     */

    /*
     * 1. Lấy vị trí theo tài khoản thành công
     * - When: Gọi hàm lấy vị trí theo id tài khoản hợp lệ
     * - Then: Trả về danh sách vị trí của tài khoản đó
     */
    @Test
    void testGetPositionByAccount_Success() {
        List<AccountPosition> accountPositions = accountPositionService.getPositionsByAccount(adminAccount.getId());
        assert(accountPositions.size() == 1);
        assert(accountPositions.get(0).getPosition().getName().equals("Leader"));
    }

    /*
     * 2. Lấy vị trí của 1 tài khoản không tồn tại
     * - WHen: Gọi hàm lấy vị trí theo id của 1 tài khoản không tồn tại
     * - Then: Ném ra ngoại lệ IllegalArgumentException 
     */
    @Test
    void testGetPositionByAccount_InvalidAccount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountPositionService.getPositionsByAccount(9999L);
        });
    }

    /*
     * 3. Lấy tài khoản theo vị trí thành công
     * - When: Gọi hàm lấy tài khoản theo id vị trí hợp lệ
     * - Then: Trả về danh sách tài khoản được gán vị trí đó
     */
    @Test
    void testGetAccountByPosition_Success() {
        List<AccountPosition> accountPositions = accountPositionService.getAccountsByPosition(member.getId());
        assert(accountPositions.size() == 1);
        assert(accountPositions.get(0).getAccount().getUsername().equals("studentuser"));
    }

    /*
     * 4. Lấy tài khoản theo 1 vị trí không tồn tại
     * - WHen: Gọi hàm lấy tài khoản theo id của 1 vị trí không tồn tại
     * - Then: Ném ra ngoại lệ IllegalArgumentException 
     */
    @Test
    void testGetAccountByPosition_InvalidPosition() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountPositionService.getAccountsByPosition(9999L);
        });
    }

    /*
     * 5. Gán vị trí cho 1 tài khoản thành công
     * - WHen: GỌi hàm gán vị trí hợp lệ cho 1 tài khoản tồn tại
     * - Then: Trả về đối tượng AccountPosition thể hiện việc gán vị trí thành công
     */
    @Test
    void testAssignPosition_Success() {
        AccountPosition assignment = accountPositionService.assignPosition(teacherAccount.getId(), leader.getId());
        assert(assignment.getAccount().getUsername().equals("teacheruser"));
        assert(assignment.getPosition().getName().equals("Leader"));
    }

    /*
     * 6. Gán vị trí cho 1 tài khoản không tồn tại
     * - When: GỌi hàm gán vị trí cho 1 tài khoản không tồn tại
     * - Then: Ném ra ngoại lệ IllegalArgumentException với message "Account not found"
     */
    @Test
    void testAssignPosition_InvalidAccount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountPositionService.assignPosition(9999L, leader.getId());
        });
    }

    /*
     * 7. Gán 1 vị trí không tồn tại cho tài khoản
     * - When: Gọi hàm gán 1 vị trí không tồn tại cho 1 tài khoản
     * - Then: Ném ra ngoại lệ IllegalArgumentException với message "Position not found"
     */
    @Test
    void testAssignPosition_InvalidPosition() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountPositionService.assignPosition(teacherAccount.getId(), 9999L);
        });
    }

    /*
     * 8. Gán 1 vị trí đã có cho tài khoản
     * - When: Gọi hàm gán 1 vị trí mà tài khoản đã được gán trước đó
     * - Then: Ném ra ngoại lệ IllegalArgumentException với message "Account already has this position"
     */
    @Test
    void testAssignPosition_AlreadyExists() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountPositionService.assignPosition(teacherAccount.getId(), mentor.getId());
        });
        assert(exception.getMessage().equals("Account already has this position"));
    }

    /*
     * 9. Xóa 1 vị trí của tài khoản thành công
     * - WHen: Gọi hàm xóa vị trí đang có của tài khoản 
     * - Then: Vị trí được xóa khỏi tài khoản, kiểm tra lại danh sách vị trí của tài khoản không còn vị trí đã xóa
     */
    @Test
    void testRemovePosition_Success() {
        accountPositionService.removePosition(teacherAccount.getId(), mentor.getId());
        List<AccountPosition> positions = accountPositionService.getPositionsByAccount(teacherAccount.getId());
        assert(positions.isEmpty());
    }

    /*
     * 10. Xóa vị trí của 1 tài khoản không tồn tại
     * - When: Gọi hàm xóa vị trí cho 1 tài khoản không tồn tại
     * - Then: Ném ra ngoại lệ IllegalArgumentException với message "Account or Position not found"
     */
    @Test
    void testRemovePosition_InvalidAccount() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountPositionService.removePosition(9999L, mentor.getId());
        });
        assert(exception.getMessage().equals("Account or Position not found"));
    }

    /*
     * 11. Xóa 1 vị trí không tồn tại của tài khoản
     * - When: Gọi hàm xóa cho 1 vị trí không tồn tại của tài khoản 
     * - Then: Ném ra ngoại lệ IllegalArgumentException với message "Account or Position not found"
     */
    @Test 
    void testRemovePosition_InvalidPosition() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountPositionService.removePosition(teacherAccount.getId(), 9999L);
        });
        assert(exception.getMessage().equals("Account or Position not found"));
    }

    /*
     * Hàm tiện ích tạo account test
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
