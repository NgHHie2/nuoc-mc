package com.example.accountservice.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public class ValidateUtilTest {

    /*
     * Danh sách các test case:
     * 1. Xác nhận trường hợp thông thường
     * 2. Xác nhận đầu vào là chữ hoa
     * 3. Xác nhận đầu vào có nhiều từ
     * 4. Xác nhận đầu vào có khoảng trắng ở đầu và cuối
     * 5. Xác nhận đầu vào null
     * 6. Xác nhận đầu vào rỗng hoặc chỉ có khoảng trắng
     * 7. Kiểm tra danh sách positionIds hợp lệ
     * 8. Kiểm tra danh sách positionIds null
     * 9. Kiểm tra danh sách positionIds rỗng
     * 10. Kiểm tra danh sách positionIds có phần tử âm
     * 11. Kiểm tra danh sách positionIds có phần tử null
     * 12. Kiểm tra danh sách positionIds có phần tử là 0
     * 13. Kiểm tra danh sách searchFields hợp lệ
     * 14. Kiểm tra danh sách searchFields null
     * 15. Kiểm tra danh sách searchFields rỗng
     * 16. Kiểm tra danh sách searchFields có khoảng trắng ở đầu và cuối
     * 17. Kiểm tra danh sách searchFields có trường không hợp lệ
     * 18. Kiểm tra danh sách searchFields có phần tử null
     * 19. Kiểm tra danh sách searchFields có phần tử trùng lặp
     * 20. Kiểm tra danh sách searchFields có tất cả các trường hợp hợp lệ
     * 21. Kiểm tra danh sách searchFields có tất cả các trường hợp không hợp lệ
     * 22. Kiểm tra danh sách searchFields có sự kết hợp giữa trường hợp hợp lệ và không hợp lệ
     */

    /*
     * 1. Xác nhận trường hợp thông thường
     * - Given: Chuỗi viết thường không có khoảng trắng thừa, không viết hoa
     * - When: Gọi hàm xác nhận cho chuỗi
     * - Then: Kết quả trả về đúng như mong đợi
     */
    @Test
    void testValidateKeyword_BasicCase() {
        String input = "oke";
        String expected = "oke";
        String result = ValidateUtil.validateKeyword(input);
        assert expected.equals(result);
    }

    /*
     * 2. Xác nhận đầu vào là chữ hoa
     * - Given: Chuỗi viết hoa không có khoảng trắng thừa
     * - When: Gọi hàm xác nhận cho chuỗi
     * - Then: Kết quả trả về đúng như mong đợi (chuyển thành chữ thường)
     */
    @Test
    void testValidateKeyword_UpperCase() {
        String input = "OKE";
        String expected = "oke";
        String result = ValidateUtil.validateKeyword(input);
        assert expected.equals(result);
    }

    /*
     * 3. Xác nhận đầu vào có nhiều từ
     * - Given: Chuỗi có nhiều từ/số, không có khoảng trắng thừa
     * - When: Gọi hàm xác nhận cho chuỗi
     * - Then: Kết quả trả về đúng như mong đợi (chuyển thành chữ thường)
     */
    @Test
    void testValidateKeyword_MultipleWordsCase() {
        String input = "Oke 123";
        String expected = "oke 123";
        String result = ValidateUtil.validateKeyword(input);
        assert expected.equals(result);
    }

    /*
     * 4. Xác nhận đầu vào có khoảng trắng ở đầu và cuối
     * - Given: Chuỗi có khoảng trắng thừa ở đầu và cuối
     * - When: Gọi hàm xác nhận cho chuỗi
     * - Then: Kết quả trả về đúng như mong đợi (loại bỏ khoảng trắng thừa và chuyển thành chữ thường)
     */
    @Test
    void testValidateKeyword_SpacesAround() {
        String input = "   Oke    ";
        String expected = "oke";
        String result = ValidateUtil.validateKeyword(input);
        assert expected.equals(result);
    }

    /*
     * 5. Xác nhận đầu vào null
     * - Given: Đầu vào là null
     * - When: Gọi hàm xác nhận cho chuỗi
     * - Then: Kết quả trả về đúng như mong đợi (trả về null)
     */
    @Test
    void testValidateKeyword_NullInput() {
        String input = null;
        String result = ValidateUtil.validateKeyword(input);
        assert result == null;
    }

    /*
     * 6. Xác nhận đầu vào rỗng hoặc chỉ có khoảng trắng
     * - Given: Đầu vào là chuỗi rỗng hoặc chỉ có khoảng trắng
     * - When: Gọi hàm xác nhận cho chuỗi
     * - Then: Kết quả trả về đúng như mong đợi (trả về null)
     */
    @Test
    void testValidateKeyword_EmptyInput() {
        String input = "    \n";
        String result = ValidateUtil.validateKeyword(input);
        assert result == null;
    }

    /*
     * 7. Kiểm tra danh sách positionIds hợp lệ
     * - Given: Danh sách positionIds hợp lệ (không null, không rỗng, không có phần tử âm hoặc null)
     * - When: Gọi hàm làm sạch danh sách
     * - Then: Kết quả trả về đúng như mong đợi (giữ nguyên danh sách ban đầu) 
     */
    @Test
    void testCleanPositionIds_BasicCase() {
        List<Long> input = new ArrayList<>(List.of(1L, 2L, 3L));
        List<Long> expected = new ArrayList<>(List.of(1L, 2L, 3L));
        List<Long> result = ValidateUtil.cleanPositionIds(input);
        assert expected.equals(result);
    }

    /*
     * 8. Kiểm tra danh sách positionIds null
     * - Given: Danh sách positionIds là null
     * - When: Gọi hàm làm sạch danh sách
     * - Then: Kết quả trả về đúng như mong đợi (trả về null)
     */
    @Test
    void testCleanPositionIds_NullInput() {
        List<Long> input = null;
        List<Long> result = ValidateUtil.cleanPositionIds(input);
        assert result == null;
    }

    /*
     * 9. Kiểm tra danh sách positionIds rỗng
     * - Given: Danh sách positionIds rỗng
     * - When: Gọi hàm làm sạch danh sách
     * - Then: Kết quả trả về đúng như mong đợi (trả về null)
     */
    @Test
    void testCleanPositionIds_EmptyInput() {
        List<Long> input = new ArrayList<>(List.of());
        List<Long> result = ValidateUtil.cleanPositionIds(input);
        assert result == null;
    }

    /*
     * 10. Kiểm tra danh sách positionIds có phần tử âm
     * - Given: Danh sách positionIds có phần tử âm
     * - When: Gọi hàm làm sạch danh sách
     * - Then: Kết quả trả về đúng như mong đợi (loại bỏ phần tử âm)
     */
    @Test
    void testCleanPositionIds_WithNegative() {
        List<Long> input = new ArrayList<>(List.of(1L, -2L, 3L));
        List<Long> expected = new ArrayList<>(List.of(1L, 3L));
        List<Long> result = ValidateUtil.cleanPositionIds(input);
        assert expected.equals(result);
    }

    /*
     * 11. Kiểm tra danh sách positionIds có phần tử null
     * - Given: Danh sách positionIds có phần tử null
     * - When: Gọi hàm làm sạch danh sách
     * - Then: Kết quả trả về đúng như mong đợi (loại bỏ phần tử null)
     */
    @Test
    void testCleanPositionsIds_WithNull() {
        List<Long> input = new ArrayList<>(List.of(1L, 3L));
        input.add(null);
        List<Long> expected = new ArrayList<>(List.of(1L, 3L));
        List<Long> result = ValidateUtil.cleanPositionIds(input);
        assert expected.equals(result);
    }

    /*
     * 12. Kiểm tra danh sách positionIds có phần tử là 0
     * - Given: Danh sách positionIds có phần tử là 0
     * - When: Gọi hàm làm sạch danh sách
     * - Then: Kết quả trả về đúng như mong đợi (loại bỏ phần tử là 0)
     */
    @Test
    void testCleanPositionIds_ZeroId() {
        List<Long> input = new ArrayList<>(List.of(1L, 0L, 3L));
        List<Long> expected = new ArrayList<>(List.of(1L, 3L));
        List<Long> result = ValidateUtil.cleanPositionIds(input);
        assert expected.equals(result);
    }

    /*
     * 13. Kiểm tra danh sách searchFields hợp lệ
     * - Given: Danh sách searchFields hợp lệ (không null, không rỗng, không có trường không hợp lệ)
     * - When: Gọi hàm xác nhận danh sách
     * - Then: Kết quả trả về đúng như mong đợi (giữ nguyên danh sách ban đầu)
     */
    @Test
    void testValidateSearchFields_BasicCase() {
        List<String> input = new ArrayList<>(List.of("username", "firstName", "email"));
        List<String> expected = new ArrayList<>(List.of("username", "firstName", "email"));
        List<String> result = ValidateUtil.validateSearchFields(input);
        assert expected.equals(result);
    }

    /*
     * 14. Kiểm tra danh sách searchFields null
     * - Given: Danh sách searchFields là null
     * - When: Gọi hàm xác nhận danh sách
     * - Then: Kết quả trả về đúng như mong đợi (trả về null)
     */
    @Test
    void testValidateSearchFields_NullInput() {
        List<String> input = null;
        List<String> result = ValidateUtil.validateSearchFields(input);
        assert result == null;
    }

    /*
     * 15. Kiểm tra danh sách searchFields rỗng
     * - Given: Danh sách searchFields rỗng
     * - When: Gọi hàm xác nhận danh sách
     * - Then: Kết quả trả về đúng như mong đợi (trả về null)
     */
    @Test
    void testValidateSearchFields_EmptyInput() {
        List<String> input = new ArrayList<>();
        List<String> result = ValidateUtil.validateSearchFields(input);
        assert result == null;
    }

    /*
     * 16. Kiểm tra danh sách searchFields có khoảng trắng ở đầu và cuối
     * - Given: Danh sách searchFields có khoảng trắng thừa ở đầu và cuối các phần tử
     * - When: Gọi hàm xác nhận danh sách
     * - Then: Kết quả trả về đúng như mong đợi (loại bỏ khoảng trắng thừa)
     */
    @Test
    void testValidateSearchFields_WithSpaces() {
        List<String> input = new ArrayList<>(List.of("  username  ", " firstName ", "   email   "));
        List<String> expected = new ArrayList<>(List.of("username", "firstName", "email"));
        List<String> result = ValidateUtil.validateSearchFields(input);
        assert expected.equals(result);
    }

    /*
     * 17. Kiểm tra danh sách searchFields có trường không hợp lệ
     * - Given: Danh sách searchFields có trường không hợp lệ
     * - When: Gọi hàm xác nhận danh sách
     * - Then: Kết quả trả về đúng như mong đợi (loại bỏ trường không hợp lệ)
     */
    @Test
    void testValidateSearchFields_WithInvalidFields() {
        List<String> input = new ArrayList<>(List.of("username", "password", "firstName", "invalidField"));
        List<String> expected = new ArrayList<>(List.of("username", "firstName"));
        List<String> result = ValidateUtil.validateSearchFields(input);
        assert expected.equals(result);
    }

    /*
     * 18. Kiểm tra danh sách searchFields có phần tử null
     * - Given: Danh sách searchFields có phần tử null
     * - When: Gọi hàm xác nhận danh sách
     * - Then: Kết quả trả về đúng như mong đợi (loại bỏ phần tử null)
     */
    @Test
    void testValidateSearchFields_WithNullValues() {
        List<String> input = new ArrayList<>(List.of("username", "firstName"));
        input.add(null);
        input.add("email");
        List<String> expected = new ArrayList<>(List.of("username", "firstName", "email"));
        List<String> result = ValidateUtil.validateSearchFields(input);
        assert expected.equals(result);
    }

    /*
     * 19. Kiểm tra danh sách searchFields có phần tử trùng lặp
     * - Given: Danh sách searchFields có phần tử trùng lặp
     * - When: Gọi hàm xác nhận danh sách
     * - Then: Kết quả trả về đúng như mong đợi (loại bỏ phần tử trùng lặp)
     */
    @Test
    void testValidateSearchFields_WithDuplicates() {
        List<String> input = new ArrayList<>(List.of("username", "firstName", "username", "email", "firstName"));
        List<String> expected = new ArrayList<>(List.of("username", "firstName", "email"));
        List<String> result = ValidateUtil.validateSearchFields(input);
        assert expected.equals(result);
    }

    /*
     * 20. Kiểm tra danh sách searchFields có tất cả các trường hợp hợp lệ
     * - Given: Danh sách searchFields có tất cả các trường hợp hợp lệ
     * - When: Gọi hàm xác nhận danh sách
     * - Then: Kết quả trả về đúng như mong đợi (giữ nguyên danh sách ban đầu)
     */
    @Test
    void testValidateSearchFields_AllAllowedFields() {
        List<String> input = new ArrayList<>(List.of("username", "firstName", "lastName", "fullName", "phoneNumber", "email", "cccd"));
        List<String> expected = new ArrayList<>(List.of("username", "firstName", "lastName", "fullName", "phoneNumber", "email", "cccd"));
        List<String> result = ValidateUtil.validateSearchFields(input);
        assert expected.equals(result);
    }

    /*
     * 21. Kiểm tra danh sách searchFields có tất cả các trường hợp không hợp lệ
     * - Given: Danh sách searchFields có tất cả các trường hợp không hợp lệ
     * - When: Gọi hàm xác nhận danh sách
     * - Then: Kết quả trả về đúng như mong đợi (trả về danh sách rỗng)
     */
    @Test
    void testValidateSearchFields_AllInvalidFields() {
        List<String> input = new ArrayList<>(List.of("password", "id", "createdAt", "updatedAt", "invalidField"));
        List<String> result = ValidateUtil.validateSearchFields(input);
        assert result.isEmpty();
    }

    /*
     * 22. Kiểm tra danh sách searchFields có sự kết hợp giữa trường hợp hợp lệ và không hợp lệ
     * - Given: Danh sách searchFields có sự kết hợp giữa trường hợp hợp lệ và không hợp lệ, có khoảng trắng thừa và phần tử null
     * - When: Gọi hàm xác nhận danh sách
     * - Then: Kết quả trả về đúng như mong đợi (giữ lại các trường hợp hợp lệ, loại bỏ các trường hợp không hợp lệ, loại bỏ khoảng trắng thừa và phần tử null)
     */
    @Test
    void testValidateSearchFields_MixedValidInvalid() {
        List<String> input = new ArrayList<>(List.of("username", "password", "  email  ", "invalidField", "cccd", "firstName"));
        input.add(null);
        List<String> expected = new ArrayList<>(List.of("username", "email", "cccd", "firstName"));
        List<String> result = ValidateUtil.validateSearchFields(input);
        assert expected.equals(result);
    }
}
