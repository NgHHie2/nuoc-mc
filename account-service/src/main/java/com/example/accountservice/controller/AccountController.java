package com.example.accountservice.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.accountservice.annotation.RequireRole;
import com.example.accountservice.dto.AccountSearchDTO;
import com.example.accountservice.dto.PasswordChangeDTO;
import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.service.AccountService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    /**
     * Tìm kiếm tài khoản theo từ khóa, theo filter role hoặc position với phân
     * trang
     * Quyền: Chỉ ADMIN mới được sử dụng
     * 
     * Input:
     * - keyword (optional): Từ khóa tìm kiếm trong username, tên, email, cccd, sdt
     * - role (optional): Lọc theo vai trò (STUDENT, TEACHER, ADMIN)
     * - positionIds (optional): Danh sách ID các vị trí
     * - searchFields (optional): Các trường cụ thể muốn tìm kiếm theo keyword
     * - pageable: Thông tin phân trang (page, size, sort)
     * 
     * Output:
     * - Page<Account>: Danh sách tài khoản phân trang với metadata
     */
    @GetMapping("/search")
    @RequireRole({ Role.ADMIN })
    public Page<Account> searchAccounts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) Role role,
            @RequestParam(value = "positionIds", required = false) List<Long> positionIds,
            @RequestParam(value = "searchFields", required = false) List<String> searchFields,
            Pageable pageable) {

        AccountSearchDTO searchDTO = new AccountSearchDTO();
        searchDTO.setKeyword(keyword);
        searchDTO.setRole(role);
        searchDTO.setPositionIds(positionIds);
        searchDTO.setSearchFields(searchFields);

        return accountService.universalSearch(searchDTO, pageable);
    }

    /**
     * Xóa tài khoản (soft delete)
     * Chức năng: Xóa mềm tài khoản (set visible = 0)
     * Quyền: Chỉ ADMIN mới được sử dụng
     * 
     * Input:
     * - id (path variable): ID của tài khoản cần xóa
     * 
     * Output:
     * - String: Thông báo kết quả xóa
     */
    @DeleteMapping("/{id}")
    @RequireRole({ Role.ADMIN })
    public String deleteAccount(@PathVariable Long id) {
        Boolean success = accountService.deleteAccount(id);
        return success ? "Tài khoản đã được xóa thành công!" : "Không tìm thấy tài khoản!";
    }

    /**
     * Lấy thông tin tài khoản theo ID
     * Chức năng: Truy xuất thông tin chi tiết của một tài khoản bất kì
     * Quyền: ADMIN và TEACHER có thể sử dụng
     * 
     * Input:
     * - id (path variable): ID của tài khoản cần lấy thông tin
     * 
     * Output:
     * - Optional<Account>: Thông tin tài khoản hoặc empty nếu không tìm thấy
     */
    @GetMapping("/{id}")
    @RequireRole({ Role.ADMIN, Role.TEACHER })
    public Optional<Account> getAccountById(@PathVariable Long id) {
        log.info("get user by id: " + id);
        return accountService.getAccountById(id);
    }

    /**
     * User xem thông tin của chính mình
     * 
     * Input:
     * - X-User-Id (header): ID của user hiện tại từ JWT token
     * 
     * Output:
     * - Optional<Account>: Thông tin tài khoản của user hiện tại
     */
    @GetMapping("/me")
    public Optional<Account> getAccountByMe(@RequestHeader(value = "X-User-Id", required = false) String userIdString) {
        Long accountId = Long.valueOf(userIdString);
        log.info("get user by id: " + accountId);
        return accountService.getAccountById(accountId);
    }

    /**
     * Tạo tài khoản mới
     * Chức năng: Tạo tài khoản mới với username và password tự động sinh
     * Quyền: Chỉ ADMIN mới được sử dụng
     * 
     * Input:
     * - account (request body): Thông tin tài khoản mới (firstName, lastName, cccd,
     * role, email...)
     * - Username sẽ được tự động sinh từ tên
     * - Password mặc định là "123456Aa@"
     * 
     * Output:
     * - Account: Thông tin tài khoản vừa được tạo (bao gồm username đã sinh)
     */
    @PostMapping
    @RequireRole({ Role.ADMIN })
    public Account createAccount(@Valid @RequestBody Account account) {
        Account savedAccount = accountService.createAccount(account);
        return savedAccount;
    }

    /**
     * Cập nhật thông tin tài khoản
     * Chức năng: Cập nhật thông tin tài khoản (không bao gồm password)
     * Quyền: Chỉ ADMIN mới được sử dụng
     * 
     * Input:
     * - id (path variable): ID của tài khoản cần cập nhật
     * - account (request body): Thông tin mới của tài khoản
     * 
     * Output:
     * - Account: Thông tin tài khoản sau khi cập nhật
     */
    @PutMapping("/{id}")
    @RequireRole({ Role.ADMIN })
    public Account updateAccount(@PathVariable Long id, @Valid @RequestBody Account account) {
        account.setId((id));
        return accountService.updateAccount(account);
    }

    /**
     * Admin đổi password cho user bất kỳ
     * Chức năng: Cho phép admin thay đổi password của bất kỳ user nào
     * Quyền: Chỉ ADMIN mới được sử dụng
     * 
     * Input:
     * - id (path variable): ID của tài khoản cần đổi password
     * - passwordChangeDTO (request body):
     * - newPassword: Mật khẩu mới (tối thiểu 6 ký tự)
     * - confirmPassword: Xác nhận mật khẩu mới
     * 
     * Output:
     * - Boolean: true nếu đổi password thành công, false nếu thất bại
     */
    @PutMapping("/change-password/{id}")
    @RequireRole({ Role.ADMIN })
    public Boolean updatePasswordByAdmin(@PathVariable Long id,
            @Valid @RequestBody PasswordChangeDTO passwordChangeDTO) {
        return accountService.updatePasswordByAdmin(id, passwordChangeDTO);
    }

    /**
     * User đổi password của chính mình
     * Chức năng: Cho phép user thay đổi password của chính mình
     * Quyền: Tất cả user đã đăng nhập
     * 
     * Input:
     * - X-User-Id (header): ID của user hiện tại từ JWT token
     * - passwordChangeDTO (request body):
     * - oldPassword: Mật khẩu cũ (bắt buộc)
     * - newPassword: Mật khẩu mới (tối thiểu 6 ký tự)
     * - confirmPassword: Xác nhận mật khẩu mới
     * 
     * Output:
     * - Boolean: true nếu đổi password thành công, false nếu thất bại
     */
    @PutMapping("/change-password")
    public Boolean updatePasswordByUser(
            @RequestHeader(value = "X-User-Id", required = false) String userIdString,
            @Valid @RequestBody PasswordChangeDTO passwordChangeDTO) {

        if (passwordChangeDTO.getOldPassword() == null || passwordChangeDTO.getOldPassword().isEmpty()) {
            throw new IllegalArgumentException("Old password must not be empty");
        }
        Long accountId = Long.valueOf(userIdString);
        return accountService.updatePasswordByUser(accountId, passwordChangeDTO);
    }

}