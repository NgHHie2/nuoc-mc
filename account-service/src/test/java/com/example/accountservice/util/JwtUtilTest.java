package com.example.accountservice.util;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
public class JwtUtilTest {

    private JwtUtil jwtUtil;
    private Account testAccount;
    private final String testSecret = "mySecretKeyForJwtTokenThatShouldBeAtLeast256BitsLongToEnsureSecurityAndProperFunctioning";
    private final long testExpiration = 86400000L; // 24 hours

    /*
     * Khởi tạo JwtUtil và thiết lập các dữ liệu cần thiết trước mỗi test
     */
    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", testExpiration);
        
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUsername("testuser");
        testAccount.setPassword("password123");
        testAccount.setFirstName("Test");
        testAccount.setLastName("User");
        testAccount.setCccd("123456789");
        testAccount.setEmail("test@example.com");
        testAccount.setPhoneNumber("0123456789");
        testAccount.setBirthDay(LocalDateTime.of(1990, 1, 1, 0, 0));
        testAccount.setRole(Role.STUDENT);
    }

    /*
     * Danh sách cần test
     * 1. gen được token, kiểm tra null không, kiểm tra đúng cấu trúc (3 phần) 
     * 2. gen token đủ thông tin account, có token id, issued at, expire 
     * 3. token đúng thời gian expire (24h) (độ lệch 1s) 
     * 4. get username from token đúng
     * 5. get userId from token đúng
     * 6. get jwtId from token được (id random)
     * 7. 2 account khác nhau gen token khác nhau
     * 8. 1 account gen 2 token khác nhau nếu khác thời gian gen
     * 9. tạo token với account null, account id null => lỗi
     * 10. get username, userId, jwtId với token sai định dạng => lỗi
     * 11. get username, userId, jwtId với token hết hạn => lỗi
     * 12. get username, userId, jwtId với token bị thay đổi payload => lỗi
     * 13. get username, userId, jwtId với token bị thay đổi signature => lỗi
     * 14. gen token với secret key khác value
     */

    /*
     * 1. gen được token, không null, đúng cấu trúc 3 phần
     * - Given: 1 Account hợp lệ
     * - When: Gọi hàm sinh token
     * - Then: TOken trả về không null, đúng cấu trúc 3 phần ngăn cách bởi dấu .
     */
    @Test
    void testGenerateToken_Success() {
        String token = jwtUtil.generateToken(testAccount);
        assertNotNull(token, "Token cannot null");
        assertTrue(token.split("\\.").length == 3, "Token must have 3 parts");
    }

    /*
     * 2. token đủ thông tin về account, có token id, thời gian tạo, thời gian hết hạn
     * - Given: 1 Account hợp lệ
     * - When: Gọi hàm sinh token
     * - Then: Token trả về có chứa các thông tin đúng về account (userId, cccd), có token id, thời gian tạo, thời gian hết hạn
     */
    @Test
    void testGenerateToken_ContainsCorrectClaims() {
        String token = jwtUtil.generateToken(testAccount);
        SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes());
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        assertEquals(testAccount.getId(), claims.get("userId", Long.class));
        assertEquals(testAccount.getId(), Long.parseLong(claims.getSubject()));
        assertEquals(testAccount.getCccd(), claims.get("cccd", String.class));
        assertNotNull(claims.getId(), "JWT ID cannot be null");
        assertNotNull(claims.getIssuedAt(), "Issued at cannot be null");
        assertNotNull(claims.getExpiration(), "Expiration cannot be null");
    }

    /*
     * 3. token đúng thời gian expire (24h) (độ lệch 1s)
     * - Given: 1 Account hợp lệ
     * - When: Gọi hàm sinh token
     * - Then: Token trả về có thời gian hết hạn là 24h (với +- 1s)
     */
    @Test
    void testGenerateToken_HasCorrectExpiration() {
        String token = jwtUtil.generateToken(testAccount);
        SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes());
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        Long diff = expiration.getTime() - issuedAt.getTime();
        assertTrue(Math.abs(diff - testExpiration) <= 1000, "Expiration time should be approximately 24 hours");
    }

    /*
     * 4. get username from token đúng
     * - Given: 1 token hợp lệ được sinh từ Account hợp lệ
     * - WHen: Gọi hàm lấy username từ token
     * - Then: Kết quả trả về đúng username của Account
     */
    @Test
    void testGetUsernameFromToken_Success() {
        String token = jwtUtil.generateToken(testAccount);
        String userId = jwtUtil.getUsernameFromToken(token);
        assertEquals(testAccount.getId().toString(), userId, "UserId from token should match account id");
    }

    /*
     * 5. get userId from token đúng
     * - Given: 1 token hợp lệ được sinh từ Account hợp lệ
     * - WWhen: Gọi hàm lấy userId từ token
     * - Then: Kết quả trả về đúng userId của Account
     */
    @Test
    void testGetUserIdFromToken_Success() {
        String token = jwtUtil.generateToken(testAccount);
        Long userId = jwtUtil.getUserIdFromToken(token);
        assertEquals(testAccount.getId(), userId, "UserId from token should match account id");
    }

    /*
     * 6. get jwtId from token được (id random)
     * - Given: 1 token hợp lệ được sinh từ Account hợp lệ
     * - When: Gọi hàm lấy jwtId từ token
     * - Then: Kết quả trả về không null (do jwtId random nên không thể xác định được giá trị)
     */
    @Test
    void testGetJwtIdFromToken_Success() {
        String token = jwtUtil.generateToken(testAccount);
        String jwtId = jwtUtil.getJwtIdFromToken(token);
        assertNotNull(jwtId, "JWT ID from token should not be null");
    }

    /*
     * 7. 2 acc khác nhau gen token khác nhau
     * - Given: 2 Account khác nhau
     * - When: Gọi hàm sinh token cho cả 2 Account
     * - Then: Kết quả trả về là 2 token khác nhau, userId, jwtId, username từ 2 token khác nhau
     */
    @Test
    void testGenerateToken_DifferentAccounts_GenerateDifferentTokens() {
        Account anotherAccount = new Account();
        anotherAccount.setId(2L);
        anotherAccount.setUsername("anotheruser");
        anotherAccount.setPassword("password456");
        anotherAccount.setFirstName("Another");
        anotherAccount.setLastName("User");
        anotherAccount.setCccd("987654321");
        anotherAccount.setEmail("another@example.com");
        anotherAccount.setPhoneNumber("0987654321");
        anotherAccount.setBirthDay(LocalDateTime.of(1992, 2, 2, 0, 0));
        anotherAccount.setRole(Role.TEACHER);

        String token1 = jwtUtil.generateToken(testAccount);
        String token2 = jwtUtil.generateToken(anotherAccount);
        assertNotEquals(token1, token2, "Tokens for different accounts should be different");

        assertNotEquals(jwtUtil.getUserIdFromToken(token1), jwtUtil.getUserIdFromToken(token2), "UserIds from tokens should be different");
        assertNotEquals(jwtUtil.getJwtIdFromToken(token1), jwtUtil.getJwtIdFromToken(token2), "JWT IDs from tokens should be different");
        assertNotEquals(jwtUtil.getUsernameFromToken(token1), jwtUtil.getUsernameFromToken(token2), "Usernames from tokens should be different");
    }

    /*
     * 8. 1 account gen 2 token khác nhau nếu khác thời gian gen
     * - Given: 1 Account hợp lệ
     * - When: Gọi hàm sinh token 2 lần với khoảng thời gian chênh lệch (1s)
     * - Then: Kết quả trả về là 2 token khác nhau, userId, username từ 2 token giống nhau, jwtId khác nhau
     */
    @Test
    void testGenerateToken_SameAccount_GeneratesDifferentTokensDueToTime() throws InterruptedException {
        String token1 = jwtUtil.generateToken(testAccount);
        TimeUnit.SECONDS.sleep(1);
        String token2 = jwtUtil.generateToken(testAccount);
        assertNotEquals(token1, token2, "Tokens generated at different times should be different");
        assertEquals(jwtUtil.getUserIdFromToken(token1), jwtUtil.getUserIdFromToken(token2), "UserIds from both tokens should be the same");
        assertEquals(jwtUtil.getUsernameFromToken(token1), jwtUtil.getUsernameFromToken(token2), "Usernames from both tokens should be the same");
        assertNotEquals(jwtUtil.getJwtIdFromToken(token1), jwtUtil.getJwtIdFromToken(token2), "JWT IDs from both tokens should be different");
    }

    /*
     * 9.1. tạo token với account null => lỗi
     * - When: Gọi hàm sinh token với tham số null
     * - Then: Ném ra ngoại lệ
     */
    @Test
    void testGenerateToken_NullAccount() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            jwtUtil.generateToken(null);
        });
        assertNotNull(exception.getMessage());
    }

    /*
     * 9.2. tạo token với account id null => lỗi
     * - Given: Account có id null
     * - When: Gọi hàm sinh token với Account có id null
     * - Then: Ném ra ngoại lệ
     */
    @Test
    void testGenerateToken_NullAccountId() {
        Account invalidAccount = new Account();
        invalidAccount.setId(null);
        invalidAccount.setUsername("invaliduser");
        invalidAccount.setPassword("password789");
        invalidAccount.setFirstName("Invalid");
        invalidAccount.setLastName("User");
        invalidAccount.setCccd("111222333");
        invalidAccount.setEmail("invalid@example.com");
        invalidAccount.setPhoneNumber("1234567890");
        invalidAccount.setBirthDay(LocalDateTime.of(1990, 1, 1, 0, 0));
        invalidAccount.setRole(Role.STUDENT);

        Exception exception = assertThrows(NullPointerException.class, () -> {
            jwtUtil.generateToken(invalidAccount);
        });
        assertNotNull(exception.getMessage());
    }

    /*
     * 10.1. get userId from token sai định dạng => lỗi
     * - Given: Token không đúng định dạng (tạm định là 1 chuỗi bất kỳ)
     * - When: Gọi hàm lấy userId từ token
     * - Then: Ném ra ngoại lệ
     */
    @Test
    void testGetUserIdFromToken_InvalidToken() {
        String token = "invalid.token.value";
        Exception exception = assertThrows(Exception.class, () -> {
            jwtUtil.getUserIdFromToken(token);
        });
        assertNotNull(exception.getMessage());
    }

    /*
     * 10.2. get username from token sai định dạng => lỗi
     * - Given: Token không đúng định dạng (tạm định là 1 chuỗi bất kỳ)
     * - When: Gọi hàm lấy username từ token
     * - Then: Ném ra ngoại lệ
     */
    @Test
    void testGetUsernameFromToken_InvalidToken() {
        String token = "invalid.token.value";
        Exception exception = assertThrows(Exception.class, () -> {
            jwtUtil.getUsernameFromToken(token);
        });
        assertNotNull(exception.getMessage());
    }

    /*
     * 10.3. get jwtId from token sai định dạng => lỗi
     * - Given: Token không đúng định dạng (tạm định là 1 chuỗi bất kỳ)
     * - When: Gọi hàm lấy jwtId từ token
     * - Then: Ném ra ngoại lệ
     */
    @Test
    void testGetJwtIdFromToken_InvalidToken() {
        String token = "invalid.token.value";
        Exception exception = assertThrows(Exception.class, () -> {
            jwtUtil.getJwtIdFromToken(token);
        });
        assertNotNull(exception.getMessage());
    }

    /*
     * 11.1. get userId từ token hết hạn => lỗi
     * - Given: Token hợp lệ nhưng đã hết hạn (tạo token với thời gian hết hạn là 1s)
     * - When: Gọi hàm lấy userId từ token sau khi token hết hạn
     * - Then: Chờ 2s thì gọi lấy userId, sau đó ném ra ngoại lệ
     */
    @Test
    void testGetUserIdFromToken_ExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 1000L); 
        String token = jwtUtil.generateToken(testAccount);
        TimeUnit.SECONDS.sleep(2); 

        Exception exception = assertThrows(Exception.class, () -> {
            jwtUtil.getUserIdFromToken(token);
        });
        assertNotNull(exception.getMessage());
    }

    /*
     * 11.2. get username từ token hết hạn => lỗi
     * - Given: Token hợp lệ nhưng đã hết hạn (tạo token với thời gian hết hạn là 1s)
     * - When: Gọi hàm lấy username từ token sau khi token hết hạn
     * - Then: Chờ 2s thì gọi lấy username, sau đó ném ra ngoại lệ
     */
    @Test
    void testGetUsernameFromToken_ExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 1000L); 
        String token = jwtUtil.generateToken(testAccount);
        TimeUnit.SECONDS.sleep(2);

        Exception exception = assertThrows(Exception.class, () -> {
            jwtUtil.getUsernameFromToken(token);
        });
        assertNotNull(exception.getMessage());
    }

    /*
     * 11.3. get jwtId từ token hết hạn => lỗi
     * - Given: Token hợp lệ nhưng đã hết hạn (tạo token với thời gian hết hạn là 1s)
     * - When: Gọi hàm lấy jwtId từ token sau khi token hết hạn
     * - Then: Chờ 2s thì gọi lấy jwtId, sau đó ném ra ngoại lệ
     */
    @Test
    void testGetJwtIdFromToken_ExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 1000L); 
        String token = jwtUtil.generateToken(testAccount);
        TimeUnit.SECONDS.sleep(2);

        Exception exception = assertThrows(Exception.class, () -> {
            jwtUtil.getJwtIdFromToken(token);
        });
        assertNotNull(exception.getMessage());
    }

    /*
     * 12.1. get userId từ token bị thay đổi payload => lỗi
     * - Given: Token hợp lệ nhưng bị thay đổi payload (thêm 1 thông tin bất kỳ vào payload)
     * - When: Gọi hàm lấy userId từ token
     * - Then: Ném ra ngoại lệ
     */
    @Test
    void testGetUserIdFromToken_ChangePayload() {
        String token = jwtUtil.generateToken(testAccount);
        String[] splits = token.split("\\.");

        String payload = new String(Base64.getUrlDecoder().decode(splits[1]));
        payload = payload.replace("}", ",\"hack\":true}");

        String modifiedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        String modifiedToken = splits[0] + "." + modifiedPayload + "." + splits[2];

        assertThrows(Exception.class, () -> {
            jwtUtil.getUserIdFromToken(modifiedToken);
        });
    }

    /*
     * 12.2. get username từ token bị thay đổi payload => lỗi
     * - Given: Token hợp lệ nhưng bị thay đổi payload (thêm 1 thông tin bất kỳ vào payload)
     * - When: Gọi hàm lấy username từ token
     * - Then: Ném ra ngoại lệ
     */
    @Test
    void testGetUsernameFromToken_ChangePayload() {
        String token = jwtUtil.generateToken(testAccount);
        String[] splits = token.split("\\.");

        String payload = new String(Base64.getUrlDecoder().decode(splits[1]));
        payload = payload.replace("}", ",\"hack\":true}");

        String modifiedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        String modifiedToken = splits[0] + "." + modifiedPayload + "." + splits[2];

        assertThrows(Exception.class, () -> {
            jwtUtil.getUsernameFromToken(modifiedToken);
        });
    }

    /*
     * 12.3. get jwtId từ token bị thay đổi payload => lỗi
     * - Given: Token hợp lệ nhưng bị thay đổi payload (thêm 1 thông tin bất kỳ vào payload)
     * - When: Gọi hàm lấy jwtId từ token
     * - Then: Ném ra ngoại lệ
     */
    @Test
    void testGetJwtIdFromToken_ChangePayLoad() {
        String token = jwtUtil.generateToken(testAccount);
        String[] splits = token.split("\\.");

        String payload = new String(Base64.getUrlDecoder().decode(splits[1]));
        payload = payload.replace("}", ",\"hack\":true}");

        String modifiedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        String modifiedToken = splits[0] + "." + modifiedPayload + "." + splits[2];

        assertThrows(Exception.class, () -> {
            jwtUtil.getJwtIdFromToken(modifiedToken);
        });
    }

    /*
     * 13.1. get userId từ token bị thay đổi signature => lỗi
     * - Given: Token hợp lệ nhưng bị thay đổi signature (thay đổi 1 ký tự bất kỳ trong phần signature)
     * - When: Gọi hàm lấy userId từ token
     * - Then: Ném ra ngoại lệ
     */
    @Test
    void testGetUserIdFromToken_ChangeSignature() {
        String token = jwtUtil.generateToken(testAccount);
        String[] splits = token.split("\\.");

        String signature = splits[2];
        String fakeSignature = "A".repeat(signature.length());
        String modifiedToken = splits[0] + "." + splits[1] + "." + fakeSignature;
        assertThrows(Exception.class, () -> {
            jwtUtil.getUserIdFromToken(modifiedToken);
        });
    }

    /*
     * 13.2. get username từ token bị thay đổi signature => lỗi
     * - Given: Token hợp lệ nhưng bị thay đổi signature (thay đổi 1 ký tự bất kỳ trong phần signature)
     * - When: Gọi hàm lấy username từ token
     * - Then: Ném ra ngoại lệ
     */
    @Test
    void testGetUsernameFromToken_ChangeSignature() {
        String token = jwtUtil.generateToken(testAccount);
        String[] splits = token.split("\\.");

        String signature = splits[2];
        String fakeSignature = "A".repeat(signature.length());
        String modifiedToken = splits[0] + "." + splits[1] + "." + fakeSignature;
        assertThrows(Exception.class, () -> {
            jwtUtil.getUsernameFromToken(modifiedToken);
        });
    }

    /*
     * 13.3. get jwtId từ token bị thay đổi signature => lỗi
     * - Given: Token hợp lệ nhưng bị thay đổi signature (thay đổi 1 ký tự bất kỳ trong phần signature)
     * - When: Gọi hàm lấy jwtId từ token
     * - Then: Ném ra ngoại lệ
     */
    @Test
    void testGetJwtIdFromToken_ChangeSignature() {
        String token = jwtUtil.generateToken(testAccount);
        String[] splits = token.split("\\.");

        String signature = splits[2];
        String fakeSignature = "A".repeat(signature.length());
        String modifiedToken = splits[0] + "." + splits[1] + "." + fakeSignature;
        assertThrows(Exception.class, () -> {
            jwtUtil.getJwtIdFromToken(modifiedToken);
        });
    }

    /*
     * 14. gen token với secret key khác value
     * - Given: 1 Account hợp lệ, 1 JwtUtil với secret key khác
     * - When: Gọi hàm sinh token
     * - Then: Ném ra ngoại lệ
     */
    @Test
    void testGenerateToken_DifferentSecretKey() {
        JwtUtil anotherJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(anotherJwtUtil, "jwtSecret", "differentSecretKey");
        assertThrows(Exception.class, () -> {
            anotherJwtUtil.generateToken(testAccount);
        });
    }

}