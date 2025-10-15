package com.example.learnservice.controller;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.server.ResponseStatusException;

import com.example.learnservice.annotation.RequireRole;
import com.example.learnservice.dto.SemesterAccountRequest;
import com.example.learnservice.dto.SemesterCreateRequest;
import com.example.learnservice.dto.SemesterUpdateRequest;
import com.example.learnservice.enums.DocumentFormat;
import com.example.learnservice.enums.Role;
import com.example.learnservice.dto.SemesterDetailDTO;
import com.example.learnservice.dto.SemesterDocumentRequest;
import com.example.learnservice.dto.SemesterResponse;
import com.example.learnservice.dto.SemesterSearchDTO;
import com.example.learnservice.dto.SemesterTeacherRequest;
import com.example.learnservice.model.Document;
import com.example.learnservice.model.Position;
import com.example.learnservice.model.Semester;
import com.example.learnservice.model.SemesterAccount;
import com.example.learnservice.model.SemesterDocument;
import com.example.learnservice.service.DocumentService;
import com.example.learnservice.service.SemesterService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/semester")
public class SemesterController {

    @Autowired
    private SemesterService semesterService;

    @Autowired
    private DocumentService documentService;

    /**
     * Lấy thông tin chi tiết của một semester
     */
    @GetMapping("/{semesterId}")
    @RequireRole({ Role.ADMIN, Role.TEACHER, Role.STUDENT })
    public ResponseEntity<Semester> getSemesterById(@PathVariable Long semesterId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {
        Long userId = Long.valueOf(userIdStr);
        Role userRole = Role.valueOf(userRoleStr);
        Position position = null;
        if (userRole.equals(Role.TEACHER) && !semesterService.checkSemesterAccessWithTeacher(semesterId, userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a teacher in this course!");
        else if (userRole.equals(Role.STUDENT)) {
            Optional<SemesterAccount> saOpt = semesterService.checkSemesterAccessWithStudent(semesterId, userId);
            if (saOpt.isEmpty())
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a student in this course!");
            position = saOpt.get().getPosition();
        }

        Semester semester = semesterService.getSemesterById(semesterId);
        if (userRole.equals(Role.STUDENT) && position != null) {
            Position pos = position;
            List<SemesterDocument> filteredDocs = semester.getSemesterDocuments().stream()
                    .filter(sd -> sd.getDocument().getCatalogs().stream()
                            .anyMatch(catalog -> catalog.getPosition().getId().equals(pos.getId())))
                    .toList();

            semester.setSemesterDocuments(filteredDocs);
        }
        return ResponseEntity.ok(semester);

    }

    /**
     * Tìm kiếm semester theo từ khóa, theo năm với phân trang
     * 
     * Input:
     * - keyword (optional): Từ khóa tìm kiếm trong name
     * - startYear (optional): Lọc theo năm bắt đầu
     * - endYear (optional): Lọc theo năm kết thúc
     * - searchFields (optional): Các trường cụ thể muốn tìm kiếm theo keyword
     * - pageable: Thông tin phân trang (page, size, sort)
     * 
     * Output:
     * - Page<SemesterDetailDTO>: Danh sách semester phân trang với metadata
     */
    @GetMapping("/search")
    @RequireRole({ Role.ADMIN, Role.TEACHER, Role.STUDENT })
    public Page<SemesterDetailDTO> searchSemesters(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "startYear", required = false) Integer startYear,
            @RequestParam(value = "endYear", required = false) Integer endYear,
            @RequestParam(value = "searchFields", required = false) List<String> searchFields,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr,
            Pageable pageable) {

        SemesterSearchDTO searchDTO = new SemesterSearchDTO();
        searchDTO.setKeyword(keyword);
        searchDTO.setStartYear(startYear);
        searchDTO.setEndYear(endYear);
        searchDTO.setSearchFields(searchFields);

        Long userId = Long.valueOf(userIdStr);
        Role userRole = Role.valueOf(userRoleStr);

        return semesterService.universalSearch(searchDTO, pageable, userId, userRole);
    }

    @PostMapping
    @RequireRole({ Role.ADMIN })
    public SemesterResponse createSemester(
            @Valid @RequestBody SemesterCreateRequest semesterCreateRequest,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {

        Long userId = Long.valueOf(userIdStr);
        Semester semester = semesterService.saveSemester(semesterCreateRequest, userId);

        SemesterResponse response = SemesterResponse.builder()
                .id(semester.getId())
                .semesterName(semester.getName())
                .startDate(semester.getStartDate())
                .endDate(semester.getEndDate())
                .createdAt(semester.getCreatedAt())
                .build();

        return response;
    }

    @PutMapping("/{semesterId}")
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    public SemesterResponse updateSemester(
            @PathVariable Long semesterId,
            @Valid @RequestBody SemesterUpdateRequest updateRequest,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {

        Long userId = Long.valueOf(userIdStr);
        Role userRole = Role.valueOf(userRoleStr);
        if (userRole.equals(Role.TEACHER) && !semesterService.checkSemesterAccessWithTeacher(semesterId, userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a teacher in this course!");
        Semester updatedSemester = semesterService.updateSemester(semesterId, updateRequest, userId);

        SemesterResponse response = SemesterResponse.builder()
                .id(updatedSemester.getId())
                .semesterName(updatedSemester.getName())
                .startDate(updatedSemester.getStartDate())
                .endDate(updatedSemester.getEndDate())
                .updatedAt(updatedSemester.getUpdatedAt())
                .build();

        return response;
    }

    @DeleteMapping("/{semesterId}")
    @RequireRole({ Role.ADMIN })
    public String deleteSemester(
            @PathVariable Long semesterId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {
        Long userId = Long.valueOf(userIdStr);
        Role userRole = Role.valueOf(userRoleStr);
        if (userRole.equals(Role.TEACHER) && !semesterService.checkSemesterAccessWithTeacher(semesterId, userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a teacher in this course!");
        semesterService.deleteSemester(semesterId, userId);

        return "Delete semester " + semesterId + " successful!";

    }

    /**
     * Thêm documents vào semester
     */
    @PostMapping("/{semesterId}/documents")
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    public String addDocumentsToSemester(
            @PathVariable Long semesterId,
            @Valid @RequestBody SemesterDocumentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {

        Long userId = Long.valueOf(userIdStr);
        Role userRole = Role.valueOf(userRoleStr);
        if (userRole.equals(Role.TEACHER) && !semesterService.checkSemesterAccessWithTeacher(semesterId, userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a teacher in this course!");
        semesterService.addDocumentsToSemester(semesterId, request, userId);

        return "Documents added successfully";
    }

    /**
     * Xóa document khỏi semester
     */
    @DeleteMapping("/{semesterId}/documents/{documentCode}")
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    public String removeDocumentFromSemester(
            @PathVariable Long semesterId,
            @PathVariable String documentCode,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {

        Long userId = Long.valueOf(userIdStr);
        Role userRole = Role.valueOf(userRoleStr);
        if (userRole.equals(Role.TEACHER) && !semesterService.checkSemesterAccessWithTeacher(semesterId, userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a teacher in this course!");
        semesterService.removeDocumentFromSemester(semesterId, documentCode, userId);

        return "Document removed successfully";
    }

    /**
     * Thêm accounts vào semester
     */
    @PostMapping("/{semesterId}/accounts")
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    public String addAccountsToSemester(
            @PathVariable Long semesterId,
            @Valid @RequestBody SemesterAccountRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {

        Long userId = Long.valueOf(userIdStr);
        Role userRole = Role.valueOf(userRoleStr);
        if (userRole.equals(Role.TEACHER) && !semesterService.checkSemesterAccessWithTeacher(semesterId, userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a teacher in this course!");
        semesterService.addAccountsToSemester(semesterId, request, userId);

        return "Accounts added successfully";
    }

    /**
     * Xóa account khỏi semester
     */
    @DeleteMapping("/{semesterId}/accounts/{accountId}")
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    public String removeAccountFromSemester(
            @PathVariable Long semesterId,
            @PathVariable Long accountId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {

        Long userId = Long.valueOf(userIdStr);
        Role userRole = Role.valueOf(userRoleStr);
        if (userRole.equals(Role.TEACHER) && !semesterService.checkSemesterAccessWithTeacher(semesterId, userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a teacher in this course!");
        semesterService.removeAccountFromSemester(semesterId, accountId, userId);

        return "Account removed successfully";
    }

    /**
     * Cập nhật position của account trong semester
     */
    @PutMapping("/{semesterId}/accounts/{accountId}/position")
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    public String updateAccountPosition(
            @PathVariable Long semesterId,
            @PathVariable Long accountId,
            @RequestParam Long positionId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {

        Long userId = Long.valueOf(userIdStr);
        Role userRole = Role.valueOf(userRoleStr);
        if (userRole.equals(Role.TEACHER) && !semesterService.checkSemesterAccessWithTeacher(semesterId, userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a teacher in this course!");
        semesterService.updateAccountPosition(semesterId, accountId, positionId, userId);

        return "Account position updated successfully";
    }

    /**
     * Thêm teachers vào semester
     */
    @PostMapping("/{semesterId}/teachers")
    @RequireRole({ Role.ADMIN })
    public String addTeachersToSemester(
            @PathVariable Long semesterId,
            @Valid @RequestBody SemesterTeacherRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestHeader(value = "X-User-Role", required = false) String userRoleStr) {

        Long userId = Long.valueOf(userIdStr);
        // Role userRole = Role.valueOf(userRoleStr);
        // if (userRole.equals(Role.TEACHER) &&
        // !semesterService.checkSemesterAccessWithTeacher(semesterId, userId))
        // throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a
        // teacher in this course!");
        semesterService.addTeachersToSemester(semesterId, request, userId);

        return "Accounts added successfully";
    }

    /*
     * Xóa teacher khỏi semester
     */
    @DeleteMapping("/{semesterId}/teachers/{teacherId}")
    @RequireRole({ Role.ADMIN })
    public String removeTeacherFromSemester(
            @PathVariable Long semesterId,
            @PathVariable Long teacherId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {

        Long userId = Long.valueOf(userIdStr);
        semesterService.removeTeacherFromSemester(semesterId, teacherId, userId);

        return "Teacher removed successfully";
    }

    @GetMapping("/{semesterId}/download/{fileCode}")
    @RequireRole({ Role.TEACHER, Role.ADMIN, Role.STUDENT })
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long semesterId,
            @PathVariable String fileCode,
            HttpServletRequest request) throws Exception {

        String cccd = request.getHeader("X-CCCD");
        String role = request.getHeader("X-User-Role");
        Long accountId = Long.valueOf(request.getHeader("X-User-Id"));
        if (role.equals(Role.STUDENT.toString())
                && !semesterService.checkDocumentAccessThroughSemester(semesterId, accountId, fileCode)) {
            return ResponseEntity.notFound().build();
        }
        Document document = documentService.getDocumentByCode(fileCode);

        Path filePath = documentService.getDocumentPath(document);

        // Thêm watermark dựa theo format
        byte[] watermarkedContent = documentService.getFileContent(filePath, document, cccd);

        String encodedFileName = URLEncoder.encode(document.getName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        String contentDisposition = "inline; filename*=UTF-8''" + encodedFileName;

        // Tạo InputStreamResource
        ByteArrayInputStream inputStream = new ByteArrayInputStream(watermarkedContent);
        InputStreamResource resource = new InputStreamResource(inputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, Files.probeContentType(filePath))
                .header("Content-Disposition", contentDisposition)
                .body(resource);

    }

    @GetMapping("/{semesterId}/stream/{fileCode}")
    @RequireRole({ Role.TEACHER, Role.ADMIN, Role.STUDENT })
    public ResponseEntity<ResourceRegion> streamVideo(
            @PathVariable Long semesterId,
            @PathVariable String fileCode,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws Exception {

        String cccd = request.getHeader("X-CCCD");
        String role = request.getHeader("X-User-Role");
        Long accountId = Long.valueOf(request.getHeader("X-User-Id"));
        if (role.equals(Role.STUDENT.toString())
                && !semesterService.checkDocumentAccessThroughSemester(semesterId, accountId, fileCode)) {
            return ResponseEntity.notFound().build();
        }

        Document document = documentService.getDocumentByCode(fileCode);
        if (document == null || document.getFormat() != DocumentFormat.VIDEO) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = documentService.getDocumentPath(document);

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        // Tạo cached watermarked file nếu chưa có
        // String cacheKey = fileCode + "_" + cccd;
        // File cachedFile = new File("D:/temp/cache_" + cacheKey + ".mp4");
        // if (!cachedFile.exists()) {
        // byte[] decryptedContent = fileUtil.decryptFile(filePath.toFile());
        // byte[] watermarkedContent = fileUtil.addVideoWatermark(decryptedContent,
        // cccd);
        // // byte[] watermarkedContent = decryptedContent;
        // Files.write(cachedFile.toPath(), watermarkedContent);
        // log.info("Created and cached watermarked video for {}", documentCode);
        // }

        FileSystemResource videoResource = new FileSystemResource(filePath);
        long contentLength = videoResource.contentLength();

        // Xử lý Range header (nếu có)
        ResourceRegion region;
        if (headers.getRange() != null && !headers.getRange().isEmpty()) {
            HttpRange range = headers.getRange().get(0);
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(5 * 1024 * 1024, end - start + 1); // chunk ~1MB
            region = new ResourceRegion(videoResource, start, rangeLength);
        } else {
            long rangeLength = Math.min(5 * 1024 * 1024, contentLength);
            region = new ResourceRegion(videoResource, 0, rangeLength);
        }

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT) // 206
                .contentType(MediaTypeFactory.getMediaType(videoResource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(region);
    }

}