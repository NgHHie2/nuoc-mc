package com.example.accountservice.util;

import com.example.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UsernameGeneratorTest {
    /* Danh sách các test case
        1. tên viết dạng bình thường (chũ hoa đầu tên), không dấu, không kí tự đặc biệt
        2. tên viết dạng bình thường nhưng có dấu
        3. tên viết toàn chữ hoa
        4. tên có chữ hoa và thường xen kẽ, lộn xộn
        5. tên có ký tự đặc biệt (@, #, $, ...)
        6. first name nhiều từ
        7. first name, last name mỗi cái chỉ có 1 từ
        8. last name có nhiều hơn 2 từ
        9. có space/tab thừa
        10. đảm bảo username là duy nhất (nếu trùng thì thêm số vào cuối)
        11. first name hoặc last name null/empty/chỉ có khoảng trắng -> đẩy ra exception
        12. getDefaultPassword trả về đúng giá trị
     * 
     */

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private UsernameGenerator usernameGenerator;

    /*
     * 1. tên viết dạng bình thường (chữ hoa đầu tên), không dấu, không kí tự đặc biệt
     * - Given: tên user không dấu, không ký tự đặc biệt
     * - When: gọi hàm sinh username
     * - Then: Kiểm tra username đúng định dạng 
     */
    @Test
    void testGenerateUsername_BasicCase() {
        String firstName = "Hiep";
        String lastName = "Nguyen Hoang";
        when(accountRepository.existsByUsernameAndVisible("hiepnh", 1)).thenReturn(false);

        String result = usernameGenerator.generateUsername(firstName, lastName);

        assertEquals("hiepnh", result);
    }

    /*
     * 2. tên viết dạng bình thường nhưng có dấu
     * - Given: tên user có dấu, không ký tự đặc biệt
     * - When: gọi hàm sinh username
     * - Then: Kiểm tra username đúng định dạng
     */
    @Test
    void testGenerateUsername_WithVietnameseAccents() {
        String firstName = "Hiệp";
        String lastName = "Nguyễn Hoàng";
        when(accountRepository.existsByUsernameAndVisible("hiepnh", 1)).thenReturn(false);

        String result = usernameGenerator.generateUsername(firstName, lastName);

        assertEquals("hiepnh", result);
    }
    
    /*
     * 3. tên viết toàn chữ hoa
     * - Given: tên user viết toàn chữ hoa
     * - When: gọi hàm sinh username
     * - Then: Kiểm tra username đúng định dạng
     */
    @Test
    void testGenerateUsername_UpperCaseInput() {
        String firstName = "HOÀNG";
        String lastName = "TRẦN VĂN";
        when(accountRepository.existsByUsernameAndVisible("hoangtv", 1)).thenReturn(false);

        String result = usernameGenerator.generateUsername(firstName, lastName);

        assertEquals("hoangtv", result);
    }

    /*
     * 4. tên có chữ hoa và thường xen kẽ, lộn xộn
     * - Given: tên user có chữ hoa và thường lộn xộn
     * - When: gọi hàm sinh username
     * - Then: Kiểm tra username đúng định dạng
     */
    @Test
    void testGenerateUsername_MixedCaseInput() {
        String firstName = "NhUNg";
        String lastName = "LÊ thỊ";
        when(accountRepository.existsByUsernameAndVisible("nhunglt", 1)).thenReturn(false);

        String result = usernameGenerator.generateUsername(firstName, lastName);

        assertEquals("nhunglt", result);
    }

    /*
     * 5. tên có ký tự đặc biệt (@, #, $, ...)
     * - Given: tên user có ký tự đặc biệt
     * - When: gọi hàm sinh username
     * - Then: Kiểm tra username đúng định dạng
     */
    @Test
    void testGenerateUsername_WithSpecialCharacters() {
        String firstName = "Đứ@&c @&";
        String lastName = "Ng!uyễ@&n Văn";
        when(accountRepository.existsByUsernameAndVisible("ducnv", 1)).thenReturn(false);

        String result = usernameGenerator.generateUsername(firstName, lastName);

        assertEquals("ducnv", result);
    }

    /*
     * 6. first name nhiều từ
     * - Given: first name có nhiều từ (>= 2)
     * - When: gọi hàm sinh username
     * - Then: Kiểm tra username đúng định dạng
     */
    @Test
    void testGenerateUsername_MultipleWordsInFirstName() {
        String firstName = "Minh Anh";
        String lastName = "Trần Thị";
        when(accountRepository.existsByUsernameAndVisible("minhanhtt", 1)).thenReturn(false);

        String result = usernameGenerator.generateUsername(firstName, lastName);

        assertEquals("minhanhtt", result);
    }

    /*
     * 7. first name, last name mỗi cái chỉ có 1 từ
     * - Given: first name, last name mỗi cái chỉ có 1 từ
     * - When: gọi hàm sinh username
     * - Then: Kiểm tra username đúng định dạng
     */
    @Test
    void testGenerateUsername_SingleWordLastName() {
        String firstName = "Nam";
        String lastName = "Nguyễn";
        when(accountRepository.existsByUsernameAndVisible("namn", 1)).thenReturn(false);

        String result = usernameGenerator.generateUsername(firstName, lastName);

        assertEquals("namn", result);
    }

    /*
     * 8. last name có nhiều hơn 2 từ
     * - Given: last name có nhiều hơn 2 từ
     * - When: gọi hàm sinh username
     * - Then: Kiểm tra username đúng định dạng
     */
    @Test
    void testGenerateUsername_MultipleWordsInLastName() {
        String firstName = "Lan";
        String lastName = "Phạm Thị Minh";
        when(accountRepository.existsByUsernameAndVisible("lanptm", 1)).thenReturn(false);

        String result = usernameGenerator.generateUsername(firstName, lastName);

        assertEquals("lanptm", result);
    }

    /*
     * 9. có space/tab thừa
     * - Given: first name, last name có space/tab thừa
     * - When: gọi hàm sinh username
     * - Then: Kiểm tra username đúng định dạng
     */
    @Test
    void testGenerateUsername_WithExtraSpaces() {
        String firstName = "  Hương  ";
        String lastName = "  Lê   Thị  ";
        when(accountRepository.existsByUsernameAndVisible("huonglt", 1)).thenReturn(false);

        String result = usernameGenerator.generateUsername(firstName, lastName);

        assertEquals("huonglt", result);
    }

    /*
     * 10. đảm bảo username là duy nhất (nếu trùng thì thêm số vào cuối) (TH1: trùng 1 lần)
     * - Given: username đã tồn tại trong hệ thống
     * - When: gọi hàm sinh username
     * - Then: Kiểm tra username được thêm số vào cuối để đảm bảo tính duy nhất
     * - Conditions: Sử dụng mock để giả lập việc kiểm tra username đã tồn tại
     */
    @Test
    void testGenerateUsername_EnsureUniqueUsername_WhenExists() {
        String firstName = "Hiep";
        String lastName = "Nguyen";
        
        when(accountRepository.existsByUsernameAndVisible("hiepn", 1)).thenReturn(true);
        when(accountRepository.existsByUsernameAndVisible("hiepn1", 1)).thenReturn(false);

        String result = usernameGenerator.generateUsername(firstName, lastName);

        assertEquals("hiepn1", result);
    }

    /*
     * 10. đảm bảo username là duy nhất (nếu trùng thì thêm số vào cuối) (TH2: trùng nhiều lần)
     * - Given: username đã tồn tại trong hệ thống nhiều lần
     * - When: gọi hàm sinh username
     * - Then: Kiểm tra username được thêm số vào cuối để đảm bảo tính duy nhất
     * - Conditions: Sử dụng mock để giả lập việc kiểm tra username đã tồn tại
     */
    @Test
    void testGenerateUsername_EnsureUniqueUsername_MultipleExists() {
        // Given
        String firstName = "Mai";
        String lastName = "Tran";
        
        when(accountRepository.existsByUsernameAndVisible("mait", 1)).thenReturn(true);
        when(accountRepository.existsByUsernameAndVisible("mait1", 1)).thenReturn(true);
        when(accountRepository.existsByUsernameAndVisible("mait2", 1)).thenReturn(true);
        when(accountRepository.existsByUsernameAndVisible("mait3", 1)).thenReturn(false);

        String result = usernameGenerator.generateUsername(firstName, lastName);

        assertEquals("mait3", result);
    }

    /*
     * 11. first name hoặc last name null/empty/chỉ có khoảng trắng -> đẩy ra exception
     * - Given: first name hoặc last name null/empty/chỉ có khoảng trắng
     * - When: gọi hàm sinh username
     * - Then: Kiểm tra exception được ném ra với thông điệp phù hợp
     * Chú thích: Các test case sau sử dụng để test các trường hợp khác nhau của first name và last name
     */
    @Test
    void testGenerateUsername_NullFirstName_ThrowsException() {
        String firstName = null;
        String lastName = "Nguyen";

        try {
            usernameGenerator.generateUsername(firstName, lastName);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
            assertEquals("First name and last name are required", exception.getMessage());
        }
    }

    @Test
    void testGenerateUsername_NullLastName_ThrowsException() {
        String firstName = "Hiep";
        String lastName = null;

        try {
            usernameGenerator.generateUsername(firstName, lastName);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
            assertEquals("First name and last name are required", exception.getMessage());
        }
    }

    @Test
    void testGenerateUsername_EmptyFirstName_ThrowsException() {
        String firstName = "";
        String lastName = "Nguyen";

        try {
            usernameGenerator.generateUsername(firstName, lastName);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
            assertEquals("First name and last name are required", exception.getMessage());
        }
    }

    @Test
    void testGenerateUsername_EmptyLastName_ThrowsException() {
        String firstName = "Hiep";
        String lastName = "";

        try {
            usernameGenerator.generateUsername(firstName, lastName);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
            assertEquals("First name and last name are required", exception.getMessage());
        }
    }

    @Test
    void testGenerateUsername_WhitespaceOnlyFirstName_ThrowsException() {
        String firstName = "   ";
        String lastName = "Nguyen";

        try {
            usernameGenerator.generateUsername(firstName, lastName);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
            assertEquals("First name and last name are required", exception.getMessage());
        }
    }

    @Test
    void testGenerateUsername_WhitespaceOnlyLastName_ThrowsException() {
        String firstName = "Hiep";
        String lastName = "   ";

        try {
            usernameGenerator.generateUsername(firstName, lastName);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
            assertEquals("First name and last name are required", exception.getMessage());
        }
    }

    /*
     * 12. getDefaultPassword trả về đúng giá trị
     * - When: gọi hàm getDefaultPassword
     * - Then: Kiểm tra giá trị trả về đúng như mong đợi
     */
    @Test
    void testGetDefaultPassword() {
        // When
        String result = usernameGenerator.getDefaultPassword();

        // Then
        assertEquals("123456Aa@", result);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

}
