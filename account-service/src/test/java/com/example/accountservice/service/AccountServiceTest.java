package com.example.accountservice.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.accountservice.dto.AccountSearchDTO;
import com.example.accountservice.dto.PasswordChangeDTO;
import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.util.UsernameGenerator;

import jakarta.persistence.EntityNotFoundException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class AccountServiceTest {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private UsernameGenerator usernameGenerator;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private Account account;

    /*
     * Chuẩn bị dữ liệu trước mỗi test:
     * - Xóa hết dữ liệu cũ trong db
     * - Tạo 1 tài khoản mẫu lưu vào db để test
     */
    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();

        account = new Account();
        account.setVisible(1);
        account.setUsername("testuser");
        account.setPassword(passwordEncoder.encode("123456Aa@"));
        account.setFirstName("Test");
        account.setLastName("User");
        account.setEmail("testuser@example.com");
        account.setCccd("123456789");
        account.setRole(Role.STUDENT);
        account = accountRepository.save(account);
    }

    /* 
     * Danh sách các test case:
     * 1. Lấy tài khoản theo id thành công
     * 2. Lấy tài khoản theo id không tồn tại
     * 3. Lấy tài khoản theo username và visible thành công
     * 4. Không tìm thấy tài khoản theo username và visible
     * 5. Kiểm tra có tồn tại tài khoản theo CCCD không (Có tồn tại)
     * 6. Kiểm tra có tồn tại tài khoản theo CCCD không (Không tồn tại)
     * 7. Kiểm tra có tồn tại tài khoản theo email không (Có tồn tại)
     * 8. Kiểm tra có tồn tại tài khoản theo email không (Không do sai input)
     * 9. Kiểm tra có tồn tại tài khoản theo email không (Không tồn tại)
     * 10. TÌm tài khoản theo email thành công
     * 11. Không tìm thấy tài khoản theo email
     * 12. Xóa tài khoản thành công
     * 13. Xóa 1 tài khoản không tồn tại
     * 14. Xóa 1 tài khoản đã bị xóa trước đó
     * 15. Tạo tài khoản thành công
     * 16. Tạo tài khoản với CCCD đã tồn tại
     * 17. Tạo tài khoản với email đã tồn tại
     * 18. Tạo tài khoản nhưng thiếu trường bắt buộc 
     * 19. Cập nhật tài khoản thành công
     * 20. Cập nhật 1 tài khoản không tồn tại
     * 21. Cập nhật tài khoản với CCCD bị trùng
     * 22. Cập nhật tài khoản với email bị trùng
     * 23. Cập nhật tài khoản đảm bảo sẽ không thay đổi password và visible
     * 24. Cập nhật mật khẩu cho tài khoản bởi Admin thành công
     * 25. Cập nhật mật khẩu cho 1 tài khoản không tồn tại bởi Admin
     * 26. Cập nhật mật khẩu cho tài khoản bởi Admin nhưng mật khẩu mới và xác nhận không khớp
     * 27. Tìm kiếm tài khoản có kết quả
     * 28. Tìm kiếm tài khoản không có kết quả
     */

    /*
     * 1. Lấy tài khoản theo id thành công
     * - When: Gọi hàm lấy tài khoản với id 
     * - Then: Trả về tài khoản có id tương ứng
     */
    @Test
    void testGetAccountById_Success() {
        Optional<Account> accountOpt = accountService.getAccountById(account.getId());

        assert(accountOpt.isPresent());
        assert(accountOpt.get().getUsername().equals("testuser"));
    }

    /*
     * 2. Lấy tài khoản không tồn tại
     * - When: Gọi hàm lấy tài khoản với id không tồn tại
     * - Then: Ném ra ngoại lệ EntityNotFoundException 
    @Test
    void testGetAccountById_NotFound() {
        assertThrows(EntityNotFoundException.class, () -> {
            accountService.getAccountById(999L);
        });
    }

    /*
     * 3. Lấy tài khoản theo username và visible thành công
     * - When: Gọi hàm lấy tài khoản với username
     * - Then: Trả về tài khoản có username tương ứng và visible = 1
     */
    @Test
    void testFindByUsernameAndVisible_Success() {
        Optional<Account> accountOpt = accountService.findByUsernameAndVisible(account.getUsername());

        assert(accountOpt.isPresent());
        assert(accountOpt.get().getUsername().equals("testuser"));
    }

    /*
     * 4. Không tìm thấy tài khoản theo username và visible
     * - When: Gọi hàm lấy tài khoản với username không tồn tại
     * - THen: Trả về Optional rỗng
     */
    @Test
    void testFindByUsernameAndVisible_NotFound() {
        Optional<Account> accountOpt = accountService.findByUsernameAndVisible("nonexistent");

        assert(accountOpt.isEmpty());
    }

    /*
     * 5. Kiểm tra có tồn tại tài khoản theo CCCD không (Có tồn tại)
     * - When: Gọi hàm kiểm tra tồn tại tài khoản theo CCCD đã có trong db
     * - Then: Trả về true vì có tài khoản tương ứng với CCCD đó
     */
    @Test
    void testExistsByCccd_True() {
        boolean exists = accountService.existsByCccd(account.getCccd());

        assertTrue(exists);
    }

    /*
     * 6. Kiểm tra có tồn tại tài khoản theo CCCD không (Không tồn tại)
     * - When: Gọi hàm kiểm tra xem có tài khoản theo CCCD không
     * - Then: Trả về false vì không có tài khoản nào ứng với CCCD đó
     */
    @Test
    void testExistsByCccd_False() {
        boolean exists = accountService.existsByCccd("nonexistent");

        assertFalse(exists);
    }

    /*
     * 7. Kiểm tra có tồn tại tài khoản theo email không (Có tồn tại)
     * - When: Gọi hàm kiểm tra xem có tồn tại tài khoản nào ứng với email không
     * - Then: Trả về true vì có tài khoản ứng với email đó
     */
    @Test 
    void testExistsByEmail_True() {
        boolean exists = accountService.existsByEmail(account.getEmail());
        assertTrue(exists);
    }

    /*
     * 8. Kiểm tra có tồn tại tài khoản theo email không (không do sai input)
     * - When: Gọi hàm kiểm tra tồn tại tài khoản với email null
     * - Then: Trả về false vì email không hợp lệ (null hoặc rỗng)
     */
    @Test
    void testExistsByEmail_False_InvalidInput() {
        boolean exists = accountService.existsByEmail(null);
        assertFalse(exists);
    }

    /*
     * 9. Kiểm tra có tồn tại tài khoản theo email không (Không tồn tại)
     * - When: Gọi hàm kiểm tra tồn tại tài khoản với email không
     * - Then: Trả về false vì không có tài khoản nào ứng với email đó
     */
    @Test
    void testExistsByEmail_False_NotFound() {
        boolean exists = accountService.existsByEmail("nonexistent@example.com");
        assertFalse(exists);
    }

    /*
     * 10. Tìm tài khoản theo email thành công
     * - When: Gọi hàm tìm tài khoản với email có trong db
     * - THen: Trả về thông tin tài khoản ứng với email đó
     */
    @Test
    void testFindByEmail_Success() {
        Optional<Account> accountOpt = accountService.findByEmail(account.getEmail());

        assert(accountOpt.isPresent());
        assert(accountOpt.get().getUsername().equals("testuser"));
    }

    /*
     * 11. Không tìm thấy tài khoản theo email
     * - When: Gọi hàm tìm tài khoản với email không có trong db
     * - THen: Trả về Optional rỗng
     */
    @Test
    void testFindByEmail_NotFound() {
        Optional<Account> accountOpt = accountService.findByEmail("nonexistent@example.com");

        assert(accountOpt.isEmpty());
    }

    /*
     * 12. Xóa tài khoản thành công
     * - When: Gọi hàm xóa tài khoản với id của tài khoản có visible = 1
     * - Then: Trả về true, kiểm tra lại tài khoản sẽ thấy visible = 0
     */
    @Test
    void testDeleteAccount_Success() {
        Boolean deleted = accountService.deleteAccount(account.getId());
        assertTrue(deleted);
        Optional<Account> deletedAccountOpt = accountRepository.findById(account.getId());
        assert(deletedAccountOpt.isPresent());
        assert(deletedAccountOpt.get().getVisible() == 0);
    }

    /*
     * 13. Xóa 1 tài khoản không tồn tại
     * - WHen: GỌi hàm xóa tài khoản với id không tồn tại
     * - THen: Ném ra ngoại lệ EntityNotFoundException 
     */
    @Test 
    void testDeleteAccount_NotFound() {
        assertThrows(EntityNotFoundException.class, () -> {
            accountService.deleteAccount(999L);
        });
    }

    /*
     * 14. Xóa 1 tài khoản đã bị xóa trước đó
     * - Given: Đổi và lưu visible của tài khoản test thành 0 (Soft Delete)
     * - When: Gọi hàm xóa tài khoản với id của tài khoản test
     * - Then: Ném ra ngoại lệ IllegalArgumentException
     */
    @Test
    void testDeleteAccount_AlreadyDeleted() {
        account.setVisible(0);
        accountRepository.save(account);

        assertThrows(IllegalArgumentException.class,() -> {
            accountService.deleteAccount(account.getId());
        });
    }

    /*
     * 15. Tạo tài khoản thành công
     * - Given: Thêm thông tin tài khoản mới chưa có trong db
     * - When: Gọi hàm tạo tài khoản với data vừa tạo
     * - Then: Trả về tài khoản mới tạo với id, username, password sinh tự động, password được mã hóa, visible = 1
     */
    @Test
    void testCreateAccount_Success() {
        Account testAccount = new Account();
        testAccount.setFirstName("New");
        testAccount.setLastName("User");
        testAccount.setEmail("newuser@example.com");
        testAccount.setCccd("987654321");

        Account createdAccount = accountService.createAccount(testAccount);

        assert(createdAccount.getId() != null);
        assert(createdAccount.getUsername().equals("newu"));
        assert(passwordEncoder.matches("123456Aa@", createdAccount.getPassword()));
        assert(createdAccount.getVisible() == 1);

    }

    /*
     * 16. Tạo tài khoản với CCCD đã tồn tại
     * - Given: THêm thông tin tài khoản mới có CCCD trùng với 1 tài khoản đã có trong db
     * - When: Gọi hàm tạo tài khoản với data vừa tạo
     * - Then: Ném ra ngoại lệ IllegalArgumentException với message "CCCD already exists"
     */
    @Test
    void testCreateAccount_DuplicateCccd() {
        Account testAccount = new Account();
        testAccount.setFirstName("New");
        testAccount.setLastName("User");
        testAccount.setEmail("newuser@example.com");
        testAccount.setCccd(account.getCccd()); 

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,() -> {
            accountService.createAccount(testAccount);
        });
        assert(exception.getMessage().equals("CCCD already exists"));
    }

    /*
     * 17. Tạo tài khoản với Email đã tồn tại
     * - Given: THêm thông tin tài khoản mới có Email trùng với 1 tài khoản đã có trong db
     * - When: Gọi hàm tạo tài khoản với data vừa tạo
     * - Then: Ném ra ngoại lệ IllegalArgumentException với message "Email already exists"
     */
    @Test
    void testCreateAccount_DuplicateEmail() {
        Account testAccount = new Account();
        testAccount.setFirstName("New");
        testAccount.setLastName("User");
        testAccount.setEmail(account.getEmail()); 
        testAccount.setCccd("987654321");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,() -> {
            accountService.createAccount(testAccount);
        });
        assert(exception.getMessage().equals("Email already exists"));
    }

    /*
     * 18. Tạo tài khoản nhưng thiếu trường bắt buộc
     * - Given: Thêm thông tin tài khoản mới nhưng thiếu trường bắt buộc (firstName, lastName, cccd) (VD: thiếu cccd)
     * - When: Gọi hàm tạo tài khoản với data vừa tạo (thiếu cccd)
     * - Then: Ném ra ngoại lệ Exception với message "CCCD is required"
     * Chú ý: Các trường bắt buộc khác cũng tương tự do các trường này có annotation @NotBlank 
     *        đã được khai báo trong entity, chỉ khác 1 chút về message trả về
     */
    @Test
    void testCreateAccount_MissingRequiredFields() {
        Account testAccount = new Account();
        testAccount.setFirstName("New");
        testAccount.setLastName("User");
        testAccount.setEmail("newuser@example.com");

        Exception exception = assertThrows(Exception.class,() -> {
            accountService.createAccount(testAccount);
        });
        assert(exception.getMessage().contains("CCCD is required"));
    }

    /*
     * 19. Cập nhật tài khoản thành công
     * - Given: Thay đổi thông tin 1 số trường của tài khoản test
     * - WHen: GỌi hàm cập nhật tài khoản với data 
     * - Then: Trả về tài khoản được cập nhật với các trường được thay đổi
     */
    @Test
    void testUpdateAccount_Success() {
        Account testAccount = new Account();
        testAccount.setId(account.getId());
        testAccount.setEmail("updateduser@example.com");
        testAccount.setCccd("123456789");

        Account updatedAccount = accountService.updateAccount(testAccount);
        assert(updatedAccount.getEmail().equals("updateduser@example.com"));
        assert(updatedAccount.getCccd().equals("123456789"));
    }

    /*
     * 20. Cập nhật 1 tài khoản không tồn tại
     * - Given: Thay đổi thông tin 1 số trường của tài khoản test, đổi id thành id không tồn tại
     * - When: Gọi hàm cập nhật tài khoản với data
     * - Then: Ném ra ngoại lệ EntityNotFoundException 
     */
    @Test
    void testUpdateAccount_NotFound() {
        Account testAccount = new Account();
        testAccount.setId(999L);
        testAccount.setEmail("updateduser@example.com");
        testAccount.setCccd("123456789");

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            accountService.updateAccount(testAccount);
        });
    }

    /*
     * 21. Cập nhật tài khoản với CCCD bị trùng
     * - Given: Tạo 1 tài khoản khác có CCCD khác tài khoản test
     *          Thay đổi CCCD của tài khoản mới thành CCCD của tài khoản test 
     * - When: Gọi hàm cập nhật tài khoản với data
     * - Then: Ném ra ngoại lệ IllegalArgumentException với message "CCCD already exists" 
     */
    @Test
    void testUpdateAccount_DuplicateCccd() {
        Account anotherAccount = new Account();
        anotherAccount.setFirstName("Another");
        anotherAccount.setLastName("User");
        anotherAccount.setEmail("anotheruser@example.com");
        anotherAccount.setCccd("987654321");
        anotherAccount.setRole(Role.STUDENT);
        anotherAccount = accountRepository.save(anotherAccount);

        Account updateAccount = new Account();
        updateAccount.setId(anotherAccount.getId());
        updateAccount.setCccd(account.getCccd()); 

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.updateAccount(updateAccount);
        });
        assert(exception.getMessage().equals("CCCD already exists"));
    }

    /*
     * 22. Cập nhật tài khoản với Email bị trùng
     * - Given: Tạo 1 tài khoản khác có Email khác tài khoản test
     *          Thay đổi Email của tài khoản mới thành Email của tài khoản test
     * - When: Gọi hàm cập nhật tài khoản với data
     * - Then: Ném ra ngoại lệ IllegalArgumentException với message "Email already exists"
     */
    @Test
    void testUpdateAccount_DuplicateEmail() {
        Account anotherAccount = new Account();
        anotherAccount.setFirstName("Another");
        anotherAccount.setLastName("User");
        anotherAccount.setEmail("anotheruser@example.com");
        anotherAccount.setCccd("987654321");
        anotherAccount.setRole(Role.STUDENT);
        anotherAccount = accountRepository.save(anotherAccount);

        
        Account updateAccount = new Account();
        updateAccount.setId(anotherAccount.getId());
        updateAccount.setEmail(account.getEmail()); 

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.updateAccount(updateAccount);
        });
        assert(exception.getMessage().equals("Email already exists"));
    }

    /*
     * 23. Cập nhật tài khoản đảm bảo sẽ không thay đổi password và visible
     * - Given: Thay đổi password và visible của tài khoản test
     * - When: Gọi hàm cập nhật tài khoản với data
     * - THen: Trả về tài khoản được cập nhật, nhưng password và visible vẫn giữ nguyên như ban đầu
     */
    @Test
    void testUpdateAccount_NotChangePasswordAndVisible() {
        Account testAccount = new Account();
        testAccount.setId(account.getId());
        testAccount.setPassword("newpassword");
        testAccount.setVisible(0);

        Account updatedAccount = accountService.updateAccount(testAccount);
        assert(passwordEncoder.matches("123456Aa@", updatedAccount.getPassword()));
        assert(updatedAccount.getVisible() == 1);
    }

    /*
     * 24. Cập nhật mật khẩu cho tài khoản bởi Admin thành công
     * - Given: Tạo PasswordChangeDTO với newPassword và confirmPassword hợp lệ và trùng nhau
     * - WHen: Gọi hàm cập nhật mật khẩu bởi ADMIN với id của tài khoản test và DTO vừa tạo
     * - Then: Trả về true, kiểm tra lại mật khẩu của tài khoản, so sánh thì thấy đã được cập nhật
     */
    @Test
    void testUpdatePasswordByAdmin_Success() {
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setNewPassword("newPassword123");
        passwordChangeDTO.setConfirmPassword("newPassword123");
        
        Boolean result = accountService.updatePasswordByAdmin(account.getId(), passwordChangeDTO);
        assertTrue(result);
        Account updatedAccount = accountRepository.findById(account.getId()).get();
        assert(passwordEncoder.matches("newPassword123", updatedAccount.getPassword()));
    }

    /*
     * 25. Cập nhật mật khẩu cho 1 tài khoản không tồn tại bởi Admin
     * - Given: Tạo PasswordChangeDTO với newPassword và confirmPassword hợp lệ và trùng nhau
     * - WHen: Gọi hàm cập nhật mật khẩu bởi ADMIN với id của tài khoản không tồn tại và DTO vừa tạo
     * - Then: Ném ra ngoại lệ EntityNotFoundException với message "Account not found"
     */
    @Test
    void testUpdatePasswordByAdmin_NotFound() {
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setNewPassword("newPassword123");
        passwordChangeDTO.setConfirmPassword("newPassword123");
        
        assertThrows(EntityNotFoundException.class, () -> {
            accountService.updatePasswordByAdmin(9999L, passwordChangeDTO);
        });
    }

    /*
     * 26. Cập nhật mật khẩu cho tài khoản bởi Admin nhưng mật khẩu mới và xác nhận không khớp
     * - Given: Tạo PasswordChangeDTO với mật khẩu mới và xác nhận khác nhau
     * - When: Gọi hàm cập nhật mật khẩu bởi ADMIN với id của tài khoản test và DTO vừa tạo
     * - Then: Ném ra ngoại lệ IllegalArgumentException với message "New password and confirm password do not match"
     */
    @Test
    void testUpdatePasswordByAdmin_NotMatch() {
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setNewPassword("newPassword123");
        passwordChangeDTO.setConfirmPassword("differentPassword");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.updatePasswordByAdmin(account.getId(), passwordChangeDTO);
        });
        assert(exception.getMessage().equals("New password and confirm password do not match"));
    }

    /*
     * 27. Tìm kiếm tài khoản có kết quả
     * - Given: Tạo AccountSearchDTO 
     * - When: Gọi hàm tìm kiếm với DTO vừa tạo
     * - Then: Trả về kết quả tìm kiếm có tài khoản test ứng với tiêu chí tìm kiếm
     */
    @Test
    void testUniversalSearch_Success() {
        AccountSearchDTO searchDTO = new AccountSearchDTO();
        searchDTO.setKeyword("test");
        Pageable pageable = Pageable.ofSize(10).withPage(0);
        
        Page<Account> results = accountService.universalSearch(searchDTO, pageable);
        assert(results.getTotalElements() == 1);
        assert(results.getContent().get(0).getUsername().equals("testuser"));
    }

    /*
     * 28. Tìm kiếm tài khoản không có kết quả
     * - Given: Tạo AccountSearchDTO với tiêu chí không có tài khoản nào ứng với tiêu chí đó
     * - When: Gọi hàm tìm kiếm với DTO vừa tạo
     * - Then: Ném ra ngoại lệ EntityNotFoundException với message "No accounts found matching the criteria"
     */
    @Test
    void testUniversalSearch_NoResults() {
        AccountSearchDTO searchDTO = new AccountSearchDTO();
        searchDTO.setKeyword("nonexistent");
        Pageable pageable = Pageable.ofSize(10).withPage(0);
        
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            accountService.universalSearch(searchDTO, pageable);
        });
        assert(exception.getMessage().equals("No accounts found matching the criteria"));
    }

    
}