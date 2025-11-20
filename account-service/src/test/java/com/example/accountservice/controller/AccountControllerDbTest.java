package com.example.accountservice.controller;

import com.example.accountservice.config.TestConfig;
import com.example.accountservice.dto.PasswordChangeDTO;
import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test cho AccountController với H2 database
 */
@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
@Transactional
class AccountControllerDbTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Account studentAccount;
    private Account teacherAccount;
    private Account adminAccount;

    /*
     * Hàm xử lý trước mỗi test case:
     * - Thiết lập MockMvc
     * - Xóa dữ liệu cũ trong database
     * - Tạo và lưu 3 tài khoản test (student, teacher, admin)
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

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
     * 1. Tìm kiếm được tài khoản (Happy Path - không filter)
     * 2. Tìm kiếm nhưng kết quả rỗng 
     * 3. Tìm kiếm với role không hợp lệ
     * 4. Tìm kiếm với filter
     * 5. Lấy tài khoản theo id (Admin) (Happy Path)
     * 6. Lấy tài khoản theo id (Teacher) (Happy Path)
     * 7. Lấy tài khoản theo id không tồn tại
     * 8. Lấy tài khoản theo id với role không hợp lệ
     * 9. Tạo tài khoản mới (Happy Path)
     * 10. Tạo tài khoản mới với CCCD đã tồn tại
     * 11. Tạo tài khoản mới với Email đã tồn tại
     * 12. Tạo tài khoản mới với role không hợp lệ
     * 13. Tạo tài khoản mới không có firstName và lastName
     * 14. Tạo tài khoản mới không có CCCD
     * 15. Tạo tài khoản mới với CCCD không hợp lệ (<9 hoặc >12)
     * 16. Cập nhật tài khoản (Happy Path)
     * 17. Cập nhật tài khoản với role không hợp lệ
     * 18. Cập nhật 1 tài khoản không tồn tại
     * 19. Cập nhật tài khoản với CCCD đã tồn tại
     * 20. Cập nhật tài khoản với Email đã tồn tại
     * 21. Xóa tài khoản (Soft Delete) (Happy Path)
     * 22. Xóa 1 tài khoản không tồn tại
     * 23. Xóa 1 tài khoản đã bị xóa trước đó
     * 24. Lấy thông tin tài khoản của chính mình (Happy Path)
     * 25. Lấy thông tin tài khoản của chính mình khi tài khoản không tồn tại
     * 26. Admin cập nhật mật khẩu cho 1 tài khoản( Happy Path)
     * 27. Admin cập nhật mật khẩu cho 1 tài khoản không tồn tại
     * 28. Admin cập nhật mật khẩu cho 1 tài khoản với mật khẩu mới và xác nhận mật khẩu không khớp
     * 29. Admin cập nhật mật khẩu cho 1 tài khoản với mật khẩu mới không hợp lệ (< 3 kí tự)
     */

    /*
     * 1. Tìm kiếm được tài khoản (Happy Path - không filter)
     *  - Given: Đã có 3 tài khoản trong database
     *  - When: Gửi request tìm kiếm (không filter)
     *  - Then: Trả về danh sách 3 tài khoản
     *  - Expected: Trả về đúng 3 tài khoản, status 200
     */
    @Test
    void searchAccounts_WithH2Database_Success() throws Exception {
        mockMvc.perform(get("/account/search")
                .header("X-User-Role", "ADMIN")
                .param("keyword", "user")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].visible").value(1));
    }

    /*
     * 2. Tìm kiếm nhưng kết quả rỗng
     *  - Given: Đã có 3 tài khoản trong database
     *  - When: Gửi request tìm kiếm với keyword không tồn tại
     *  - Then: Trả về Entity Not Found
     *  - Expected: Trả về status 404 với message "Entity Not Found"
     */
    @Test
    void searchAccounts_WithH2Database_NoResults() throws Exception {
        mockMvc.perform(get("/account/search")
                .header("X-User-Role", "ADMIN")
                .param("keyword", "nonexistent")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("error").value("Entity Not Found"));
    }

    /*
     * 3. Tìm kiếm với role không hợp lệ
     *  - Given: Đã có 3 tài khoản trong database
     *  - When: Gửi request tìm kiếm với role không hợp lệ (khác Admin)
     *  - Then: Trả về Forbidden
     *  - Expected: Trả về status 403 với message "Permission denied"
     */
    @Test
    void searchAccounts_WithH2Database_InvalidRole() throws Exception {
        mockMvc.perform(get("/account/search")
                .header("X-User-Role", "INVALID_ROLE")
                .param("keyword", "user")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("message").value("Permission denied"));
    }
    
    /*
     * 4. Tìm kiếm với filter
     *  - Given: Đã có 3 tài khoản trong database
     *  - When: Gửi request tìm kiếm với  filter ROLE = STUDENT
     *  - Then: Trả về User có ROLE là STUDENT
     *  - Expected: Chỉ trả về 1 user duy nhất là student, status 200
     */
    @Test
    void searchAccounts_WithRoleFilter() throws Exception {
        mockMvc.perform(get("/account/search")
                .header("X-User-Role", "ADMIN")
                .param("role", "STUDENT")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].role").value("STUDENT"))
                .andExpect(jsonPath("$.content[0].username").value("student"));
    }

    /*
     * 5. Lấy tài khoản theo id (Admin) (Happy Path)
     *  - Given: Đã có 3 tài khoản trong database
     *  - When: Gửi request lấy tài khoản theo id của student với role là ADMIN
     *  - Then: Trả về thông tin tài khoản của student
     *  - Expected: Trả về đúng thông tin tài khoản, status 200
     */
    @Test
    void getAccountById_WithH2Database_AdminSuccess() throws Exception {
        mockMvc.perform(get("/account/" + studentAccount.getId().toString())
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentAccount.getId()))
                .andExpect(jsonPath("$.username").value("student"))
                .andExpect(jsonPath("$.firstName").value("Student"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    /*
     * 6. Lấy tài khoản theo id (Teacher) (Happy Path)
     *  - Given: Đã có 3 tài khoản trong database
     *  - When: Gửi Request lấy tài khoản theo id của student với role là TEACHER
     *  - Then: Trả về thông tin tài khoản của student
     *  - Expected: Trả về đúng thông tin tài khoản, status 200
     */
    @Test
    void getAccountById_WithH2Database_TeacherSuccess() throws Exception {
        mockMvc.perform(get("/account/" + studentAccount.getId().toString())
                .header("X-User-Role", "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentAccount.getId()))
                .andExpect(jsonPath("$.username").value("student"))
                .andExpect(jsonPath("$.firstName").value("Student"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    /*
     * 7. Lấy tài khoản theo id không tồn tại
     *  - Given: Đã có 3 tài khoản trong database
     *  - When: Gửi request lấy tài khoản với id không tồn tại
     *  - Then: Trả về Entity Not Found
     *  - Expected: Trả về status 404 với message "Entity Not Found"
     */
    @Test
    void getAccountById_WithH2Database_InvalidAccount() throws Exception {
        mockMvc.perform(get("/account/" + 9999)
                .header("X-User-Role", "TEACHER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Entity Not Found"));
    }

    /*
     * 8. Lấy tài khoản theo id với role không hợp lệ
     *  - Given: Đã có 3 tài khoản trong database
     *  - When: Gửi request lấy tài khoản với role là STUDENT 
     *  - Then: Trả về Permission Denied
     *  - Expected: Trả về status 403 với message "Permission denied"
     */
    @Test
    void getAccountById_WithH2Database_InvalidRole() throws Exception {
        mockMvc.perform(get("/account/" + studentAccount.getId().toString())
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Permission denied"));
    }

    /*
     * 9. Tạo tài khoản mới (Happy Path)
     *  - Given: Đã có 3 tài khoản trong database
     *  - When: Gửi request tạo tài khoản mới với thông tin hợp lệ
     *  - Then: Tạo mới thành công và lưu và db
     *  - Expected: Trả về thông tin tài khoản mới tạo, status 200, database có thêm 1 tài khoản (3 -> 4)
     */
    @Test
    void createAccount_WithH2Database_Success() throws Exception {
        Account newAccount = new Account();
        newAccount.setFirstName("New");
        newAccount.setLastName("User");
        newAccount.setEmail("new@example.com");
        newAccount.setCccd("444444444");
        newAccount.setRole(Role.STUDENT);

        mockMvc.perform(post("/account")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAccount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.cccd").value("444444444"));

        List<Account> accounts = accountRepository.findAll();
        assert accounts.size() == 4;
    }

    /*
     * 10. Tạo tài khoản mới với CCCD đã tồn tại
     *  - Given: Đã có 3 tài khoản trong db
     *  - When: Gửi request tạo tài khoản mới với CCCD đã tồn tại (trùng CCCD với studentAccount)
     *  - Then: Trả về CCCD already exists
     *  - Expected: Trả về status 400 với message "CCCD already exists", db không đổi
     */
    @Test
    void createAccount_WithDuplicateCCCD_ShouldFail() throws Exception {
        Account newAccount = new Account();
        newAccount.setFirstName("Duplicate");
        newAccount.setLastName("User");
        newAccount.setCccd("111111111");
        newAccount.setRole(Role.STUDENT);

        mockMvc.perform(post("/account")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAccount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Request"))
                .andExpect(jsonPath("$.message").value("CCCD already exists"));

        List<Account> accounts = accountRepository.findAll();
        assert accounts.size() == 3;
    }

    /*
     * ĐANG TẠM KHÔNG SỬ DỤNG EMAIL KHI TẠO TÀI KHOẢN
     * 11. Tạo tài khoản mới với Email đã tồn tại
     *  - Given: Đã có 3 tài khoản trong db
     *  - When: Gửi request tạo tài khoản mới với Email đã tồn tại (trùng email với studentAccount)
     *  - Then: Trả về Email already exists
     *  - Expected: Trả về status 400 với message "Email already exists", db không đổi
     */
    // @Test
    // void createAccount_WithDuplicateEmail_ShouldFail() throws Exception {
    //     Account newAccount = new Account();
    //     newAccount.setFirstName("Duplicate");
    //     newAccount.setLastName("User");
    //     newAccount.setCccd("444444444"); 
    //     newAccount.setEmail("student@example.com");
    //     newAccount.setRole(Role.STUDENT);

    //     mockMvc.perform(post("/account")
    //             .header("X-User-Role", "ADMIN")
    //             .contentType(MediaType.APPLICATION_JSON)
    //             .content(objectMapper.writeValueAsString(newAccount)))
    //             .andExpect(status().isBadRequest())
    //             .andExpect(jsonPath("$.error").value("Invalid Request"))
    //             .andExpect(jsonPath("$.message").value("Email already exists"));

    //     List<Account> accounts = accountRepository.findAll();
    //     assert accounts.size() == 3;
    // }

    /*
     * 12. Tạo tài khoản mới với role không hợp lệ
     *  - Given: Đã có 3 tài khoản trong db
     *  - When: Gửi request tạo tài khoản mới bằng role TEACHER (khác ADMIN)
     *  - Then: Trả về Permission Denied
     *  - Expected: Trả về status 403 với message "Permission Denied", db không đổi
     */
    @Test
    void createAccount_InvalidRole() throws Exception {
        Account newAccount = new Account();
        newAccount.setFirstName("New");
        newAccount.setLastName("User");
        newAccount.setCccd("444444444"); 
        newAccount.setRole(Role.STUDENT);

        mockMvc.perform(post("/account")
                .header("X-User-Role", "TEACHER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAccount)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Permission denied"));

        List<Account> accounts = accountRepository.findAll();
        assert accounts.size() == 3;
    }

    /*
     * 13. Tạo tài khoản mới không có firstName và lastName
     *  - Given: Đã có 3 tài khoản trong db
     *  - When: Gửi request tạo tài khoản mới nhưng thiếu firstName và lastName
     *  - Then: Trả về lỗi validate
     *  - Expected: Trả về status 400 với message tương ứng, db không đổi
     */
    @Test
    void createAccount_WithMissingName_ShouldFail() throws Exception {
        Account newAccount = new Account();
        newAccount.setFirstName(null);
        newAccount.setLastName(null);
        newAccount.setCccd("999999999"); 
        newAccount.setEmail("new@example.com");
        newAccount.setRole(Role.STUDENT);

        mockMvc.perform(post("/account")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAccount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.lastName").value("Last name is required"))
                .andExpect(jsonPath("$.firstName").value("First name is required"));

        List<Account> accounts = accountRepository.findAll();
        assert accounts.size() == 3;
    }

    /*
     * 14. Tạo tài khoản mới không có CCCD
     *  - Given: Đã có 3 tài khoản trong db
     *  - When: Gửi request tạo tài khoản mới nhưng CCCD để trống
     *  - Then: Trả về lỗi validate
     *  - Expected: Trả về status 400 với message tương ứng, db không đổi
     */
    @Test
    void createAccount_MissingCCCD_ShouldFail() throws Exception {
        Account newAccount = new Account();
        newAccount.setFirstName("New");
        newAccount.setLastName("User");
        newAccount.setCccd(null); 
        newAccount.setEmail("new@example.com");
        newAccount.setRole(Role.STUDENT);

        mockMvc.perform(post("/account")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAccount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cccd").value("CCCD is required"));

        List<Account> accounts = accountRepository.findAll();
        assert accounts.size() == 3;
    }

    /*
     * 15. Tạo tài khoản mới với CCCD không hợp lệ (<9 hoặc >12 kí tự)
     *  - Given: Đã có 3 tài khoản trong db
     *  - When: Gửi request tạo tài khoản mới với CCCD không hợp lệ
     *  - Then: Trả về lỗi validate
     *  - Expected: Trả về status 400 với message tương ứng, db không đổi
     */
    @Test
    void createAccount_InvalidCCCD_ShouldFail() throws Exception {
        Account newAccount = new Account();
        newAccount.setFirstName("New");
        newAccount.setLastName("User");
        newAccount.setCccd("123");
        newAccount.setEmail("new@example.com");
        newAccount.setRole(Role.STUDENT);

        mockMvc.perform(post("/account")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAccount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cccd").value("CCCD must be between 9 and 12 characters"));

        List<Account> accounts = accountRepository.findAll();
        assert accounts.size() == 3;
    }

    /*
     * 16. Cập nhật tài khoản (Happy Path)
     * - Given: Đã có 3 tài khoản trong db
     * - When: GỬi request cập nhật tài khoản với thông tin cập nhật hợp lệ
     * - Then: Cập nhật thành công và lưu vào db
     * - Expected: Trả về thông tin tài khoản đã cập nhật, status 200, db thay đổi đúng
     */
    @Test
    void updateAccount_WithH2Database_Success() throws Exception {
        Account updateRequest = new Account();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Student");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setCccd("111111111"); 
        updateRequest.setRole(Role.TEACHER); 

        mockMvc.perform(put("/account/" + studentAccount.getId())
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentAccount.getId()))
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.role").value("TEACHER"));

        Account updatedAccount = accountRepository.findById(studentAccount.getId()).orElse(null);
        assert updatedAccount != null;
        assert updatedAccount.getFirstName().equals("Updated");
        assert updatedAccount.getRole() == Role.TEACHER;
    }

    /*
     * 17. Cập nhật tài khoản với role không hợp lệ
     * - Given: Đã có 3 tài khoản trong db
     * - When: Gửi request cập nhật tài khoản bằng role TEACHER (khác ADMIN)
     * - Then: Trả về Permission Denied
     * - Expected: Trả về status 403 với mêssage "Permission Denied", db không đổi
     */
    @Test
    void updateAccount_WithH2Database_InvalidRole() throws Exception {
        Account updateRequest = new Account();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Student");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setCccd("111111111");
        updateRequest.setRole(Role.TEACHER);

        mockMvc.perform(put("/account/" + studentAccount.getId())
                .header("X-User-Role", "TEACHER") 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Permission denied"));

        Account updatedAccount = accountRepository.findById(studentAccount.getId()).orElse(null);
        assert updatedAccount != null;
        assert updatedAccount.getFirstName().equals("Student");
        assert updatedAccount.getRole() == Role.STUDENT;
    }

    /*
     * 18. Cập nhật 1 tài khoản không tồn tại
     * - Given: Đã có 3 tài khoản trong db
     * - When: Gửi request cập nhật 1 tài khoản với id không tồn tại
     * - Then: Trả về Entity Not Found
     * - Expected: Trả về status 404 với message "Entity Not Found", db không đổi
     */
    @Test
    void updateAccount_WithH2Database_InvalidAccount() throws Exception {
        Account updateRequest = new Account();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Student");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setCccd("111111111"); 
        updateRequest.setRole(Role.TEACHER); 

        mockMvc.perform(put("/account/" + 9999)
                .header("X-User-Role", "ADMIN") 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Entity Not Found"));

        Account updatedAccount = accountRepository.findById(studentAccount.getId()).orElse(null);
        assert updatedAccount != null;
        assert updatedAccount.getFirstName().equals("Student");
        assert updatedAccount.getRole() == Role.STUDENT;
    }

    /*
     * 19. Cập nhật tài khoản với CCCD đã tồn tại
     * - Given: Đã có 3 tài khoản trong db
     * - When: Gửi request cập nhật tài khoản với CCCD trùng với tài khoản khác
     * - Then: Trả về CCCD already exists
     * - Expected: Trả về status 400 với message "CCCD already exists", db không đổi
     */
    @Test
    void updateAccount_WithH2Database_InvalidCCCD() throws Exception {
        Account updateRequest = new Account();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Student");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setCccd("333333333"); 
        updateRequest.setRole(Role.TEACHER);

        mockMvc.perform(put("/account/" + studentAccount.getId())
                .header("X-User-Role", "ADMIN") 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CCCD already exists"))
                .andExpect(jsonPath("$.error").value("Invalid Request"));

        Account updatedAccount = accountRepository.findById(studentAccount.getId()).orElse(null);
        assert updatedAccount != null;
        assert updatedAccount.getFirstName().equals("Student");
        assert updatedAccount.getRole() == Role.STUDENT;
    }

    /*
     * 20. Cập nhật tài khoản với Email đã tồn tại
     * - Given: Đã có 3 tài khoản trong db
     * - When: Gửi request cập nhật tài khoản với Email trùng với tài khoản khác
     * - Then: Trả về Email already exists
     * - Expected: Trả về status 400 với message "Email already exists", db không đổi
     */
    @Test
    void updateAccount_WithH2Database_InvalidEmail() throws Exception {
        Account updateRequest = new Account();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Student");
        updateRequest.setEmail("admin@example.com");
        updateRequest.setCccd("111111111");
        updateRequest.setRole(Role.TEACHER); 

        mockMvc.perform(put("/account/" + studentAccount.getId())
                .header("X-User-Role", "ADMIN") 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"))
                .andExpect(jsonPath("$.error").value("Invalid Request"));

        Account updatedAccount = accountRepository.findById(studentAccount.getId()).orElse(null);
        assert updatedAccount != null;
        assert updatedAccount.getFirstName().equals("Student");
        assert updatedAccount.getRole() == Role.STUDENT;
    }

    /*
     * 21. Xóa tài khoản (Soft Delete) (Happy Path)
     * - Given: Đã có 3 tài khoản trong db
     * - When: Gửi request xóa tài khoản với id hợp lệ, chưa bị xóa trước đó
     * - Then: Xóa tài khoản thành công (thay đổi visible = 0)
     * - Êxpected: Trả về message "Tài khoản đã được xóa thành công!", status 200, db thay đổi, 
     * dùng hàm tìm kiếm không thấy
     */
    @Test
    void deleteAccount_WithH2Database_SoftDelete() throws Exception {
        mockMvc.perform(delete("/account/" + studentAccount.getId())
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account Deleted Successfully!"));

        Account deletedAccount = accountRepository.findById(studentAccount.getId()).orElse(null);
        assert deletedAccount != null;
        assert deletedAccount.getVisible() == 0; 

        assert accountRepository.findByIdAndVisible(studentAccount.getId(), 1).isEmpty();
    }

    /*
     * 22. Xóa 1 tài khoản không tồn tại
     * - Given: Đã có 3 tài khoản trong db
     * - WHen: Gửi request xóa tài khoản với id không tồn tịa trong db
     * - THen: Trả về Entity Not Found
     * - Expected: Trả về status 404 với message "Entity Not Found"
     */
    @Test
    void deleteAccount_WithH2Database_InvalidUser() throws Exception {
        mockMvc.perform(delete("/account/" + 9999)
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Entity Not Found"));
    }

    /*
     * 23. Xóa 1 tài khoản đã bị xóa trước đó
     * - Given: Đã có 3 tài khoản trong db, có 1 tài khoản đã bị xóa
     * - When: Gửi request xóa tài khoản đã bị xóa
     * - Then: Trả về Account already deleted
     * - Expected: Trả về status 400 với message "Account already deleted"
     */
    @Test
    void deleteAccount_WithH2Database_AlreadyDeleted() throws Exception {
        studentAccount.setVisible(0);
        accountRepository.save(studentAccount);

        Account deletedAccount = accountRepository.findById(studentAccount.getId()).orElse(null);
        assert deletedAccount != null;
        assert deletedAccount.getVisible() == 0; 

        assert accountRepository.findByIdAndVisible(studentAccount.getId(), 1).isEmpty();
        
        mockMvc.perform(delete("/account/" + studentAccount.getId())
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Request"));
    }

    /*
     * 24. Lấy thông tin tài khoản của chính mình (Happy Path)
     * - Given: Đã có 3 tài khoản trong db
     * - WHen: Gửi request lấy thông tin tài khoản của chính mình với id hợp lệ
     * - Then: Trả về thông tin tài khoản
     * - Expected: Trả về đúng thông tin tài khoản, status 200
     */
    @Test
    void getAccountByMe_WithH2Database_Success() throws Exception {
        mockMvc.perform(get("/account/me")
                .header("X-User-Id", studentAccount.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentAccount.getId()))
                .andExpect(jsonPath("$.username").value("student"));
    }

    /*
     * 25. Lấy thông tin tài khoản của chính mình khi tài khoản không tồn tại
     * - Given: Đã có 3 tài khoản trong db
     * - When: Gửi request lấy thông tin tài khoản của mình với id không tồn tại
     * - Then: Trả về Entity NOt Found
     * - Expected: Trả về status 404 với message "Entity Not Found"
     */
    @Test
    void getAccountByMe_WithH2Database_AccountNotFound() throws Exception {
        mockMvc.perform(get("/account/me")
                .header("X-User-Id", "9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Entity Not Found"));
    }

    /*
     * 26. Admin cập nhật mật khẩu cho 1 tài khoản (Happy Path)
     * - Given: Đã có 3 tài khoản trong db
     * - When: Gửi request cập nhật mật khẩu cho 1 tài khoản với thông tin hợp lệ
     * - Then: cập nhật mật khẩu thành công
     * - Expected: Trả về true, status 200
     */
    @Test
    void updatePasswordByAdmin_WithH2Database_Success() throws Exception {
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setNewPassword("newPassword123");
        passwordChangeDTO.setConfirmPassword("newPassword123");
        
        mockMvc.perform(put("/account/change-password/" + studentAccount.getId())
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordChangeDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    /*
     * 27. Admin cập nhật mật khẩu cho 1 tài khoản không tồn tại
     * - Given: Đã có 3 tài khoản trong db
     * - When: Gửi request cập nhật mật khẩu cho 1 tài khoản có id không tồn tịa trong db
     * - Then: Trả về Entity Not Found
     * - Expected: Trả về status 404 với message "Entity Not Found"
     */
    @Test
    void updatePasswordByAdmin_WithH2Database_InvalidAccount() throws Exception {
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setNewPassword("newPassword123");
        passwordChangeDTO.setConfirmPassword("newPassword123");
        
        mockMvc.perform(put("/account/change-password/" + 9999)
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordChangeDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Entity Not Found"));
    }

    /*
     * 28. Admin cập nhật mật khẩu cho 1 tài khoản với mật khẩu mới và xác nhận mật khẩu không khớp
     * - Given: Đã có 3 tài khoản trong db
     * - When: Gửi request cập nhật mật khẩu cho 1 tài khoản với thông tin mật khẩu không trùng khớp
     * - Then: Trả về lỗi không trùng khớp
     * - Expected: Trả về status 400 với message "New password and confirm password do not match"
     */
    @Test
    void updatePasswordByAdmin_WithH2Database_PasswordNotMatch() throws Exception {
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setNewPassword("newPassword123");
        passwordChangeDTO.setConfirmPassword("differentPassword123");
        
        mockMvc.perform(put("/account/change-password/" + studentAccount.getId())
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordChangeDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Request"))
                .andExpect(jsonPath("$.message").value("New password and confirm password do not match"));
    }

    /*
     * 29. Admin cập nhật mật khẩu cho 1 tài khoản với mật khẩu mới không hợp lệ (< 6 kí tự)
     * - Given: Đã có 3 tài khoản trong db
     * - When: Gửi request cập nhật mật khẩu cho 1 tài khoản với thông tin mật khẩu không hợp lệ
     * - Then: Trả về lỗi validate
     * - Expected: Trả về status 400 với message tương ứng
     */
    @Test
    void updatePasswordByAdmin_WithH2Database_InvalidPassword() throws Exception {
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setNewPassword("new");
        passwordChangeDTO.setConfirmPassword("new");
        
        mockMvc.perform(put("/account/change-password/" + studentAccount.getId())
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordChangeDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.newPassword").value("Password must be at least 6 characters"))
                .andExpect(jsonPath("$.confirmPassword").value("Password must be at least 6 characters"));
    }


    /*
     * Hàm tiện ích để tạo Account
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