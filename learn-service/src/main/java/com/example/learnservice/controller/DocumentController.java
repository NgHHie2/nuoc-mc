package com.example.learnservice.controller;

import com.example.learnservice.annotation.RequireRole;
import com.example.learnservice.dto.CatalogUpdateRequest;
import com.example.learnservice.dto.DocumentSearchDTO;
import com.example.learnservice.dto.DocumentUpdateRequest;
import com.example.learnservice.dto.DocumentUploadRequest;
import com.example.learnservice.enums.DocumentFormat;
import com.example.learnservice.enums.Role;
import com.example.learnservice.model.Catalog;
import com.example.learnservice.model.Document;
import com.example.learnservice.model.Tag;
import com.example.learnservice.repository.DocumentRepository;
import com.example.learnservice.service.DocumentService;
import com.example.learnservice.util.FileUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/document")
public class DocumentController {
    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private DocumentService documentService;

    @RequireRole({ Role.TEACHER, Role.ADMIN })
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadFile(
            @Valid @ModelAttribute DocumentUploadRequest uploadRequest,
            HttpServletRequest request) {
        try {
            // Lấy User ID từ header
            String userIdHeader = request.getHeader("X-User-Id");
            Long userId = Long.valueOf(userIdHeader);
            // System.out.println(document);

            Document savedDocument = documentService.processAndSaveDocument(uploadRequest.getFile(), userId,
                    uploadRequest);

            // Trả về thông tin file
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedDocument.getId());
            response.put("code", savedDocument.getCode());
            response.put("fileName", savedDocument.getName());
            // response.put("filePath", savedDocument.getFilePath());
            response.put("fileSize", savedDocument.getSize());
            response.put("fileType", savedDocument.getFormat());
            response.put("pages", savedDocument.getPages());
            response.put("minutes", savedDocument.getMinutes());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }
    }

    @RequireRole({ Role.TEACHER, Role.ADMIN })
    @GetMapping("/download/{fileCode}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileCode,
            HttpServletRequest request) throws Exception {

        // kiểm tra request đến từ nguồn url nào
        String referer = request.getHeader("referer");
        String origin = request.getHeader("origin");
        String allowed = "http://localhost:3000";

        boolean allowedRequest = (referer != null && referer.startsWith(allowed))
                || (origin != null && origin.startsWith(allowed));

        if (!allowedRequest) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("X-CCCD: " + request.getHeader("X-CCCD"));
        String cccd = request.getHeader("X-CCCD");
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

    /**
     * Stream video với hỗ trợ HTTP Range requests
     * 
     */
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    @GetMapping("/stream/{fileCode}")
    public ResponseEntity<ResourceRegion> streamVideo(
            @PathVariable String fileCode,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws Exception {

        String cccd = request.getHeader("X-CCCD");

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

    /**
     * Get preview image
     * 
     * @throws IOException
     */
    @GetMapping("/preview/{documentCode}")
    public ResponseEntity<byte[]> getPreview(@PathVariable String documentCode) throws IOException {
        Path previewPath = documentService.getPreviewPath(documentCode);
        byte[] previewBytes = fileUtil.getPreviewImage(previewPath);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(previewBytes);
    }

    /**
     * Tìm kiếm tài liệu theo từ khóa, theo format hoặc catalog với phân trang
     * Quyền: Có thể cho phép tất cả user hoặc chỉ ADMIN/TEACHER tùy yêu cầu
     * 
     * Input:
     * - keyword (optional): Từ khóa tìm kiếm trong name, documentNumber, tags
     * - format (optional): Lọc theo định dạng (PDF, VIDEO)
     * - catalogIds (optional): Danh sách ID các position (thông qua catalog)
     * - searchFields (optional): Các trường cụ thể muốn tìm kiếm theo keyword
     * - pageable: Thông tin phân trang (page, size, sort)
     * 
     * Output:
     * - Page<Document>: Danh sách tài liệu phân trang với metadata
     */
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    @GetMapping("/search")
    public Page<Document> searchDocuments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "format", required = false) DocumentFormat format,
            @RequestParam(value = "positionIds", required = false) List<Long> positionIds,
            @RequestParam(value = "searchFields", required = false) List<String> searchFields,
            Pageable pageable) {

        DocumentSearchDTO searchDTO = new DocumentSearchDTO();
        searchDTO.setKeyword(keyword == null ? null : keyword.trim());
        searchDTO.setFormat(format);
        searchDTO.setPositionIds(positionIds);
        searchDTO.setSearchFields(searchFields);
        Page<Document> documents = documentService.universalSearch(searchDTO, pageable);
        System.out.println(documents);
        return documents;
    }

    @RequireRole({ Role.TEACHER, Role.ADMIN, Role.STUDENT })
    @GetMapping("/{code}")
    public Document getDocumentByCode(@PathVariable String code) {
        log.info("get document by code: " + code);
        return documentService.getDocumentByCode(code);
    }

    @RequireRole({ Role.TEACHER, Role.ADMIN })
    @GetMapping("/number/{documentNumber}")
    public ResponseEntity<Document> getDocumentByDocumentNumber(@PathVariable String documentNumber) {
        log.info("get document by number: " + documentNumber);
        Document document = documentService.getDocumentByDocumentNumber(documentNumber);
        if (document == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(document);
    }

    /**
     * Cập nhật thông tin tài liệu
     */
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    @PutMapping("/{documentCode}")
    public ResponseEntity<?> updateDocument(
            @PathVariable String documentCode,
            @Valid @RequestBody DocumentUpdateRequest updateRequest,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long userId = Long.valueOf(userIdStr);
        Document updatedDocument = documentService.updateDocument(documentCode, updateRequest, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", updatedDocument.getId());
        response.put("code", updatedDocument.getCode());
        response.put("name", updatedDocument.getName());
        response.put("documentNumber", updatedDocument.getDocumentNumber());
        response.put("description", updatedDocument.getDescription());
        response.put("format", updatedDocument.getFormat());
        response.put("updatedAt", updatedDocument.getUpdatedAt());

        // Tags
        if (updatedDocument.getTags() != null) {
            List<String> tagNames = updatedDocument.getTags().stream()
                    .map(Tag::getName)
                    .toList();
            response.put("tags", tagNames);
        }

        return ResponseEntity.ok(response);

    }

    /**
     * Xóa tài liệu
     */
    @RequireRole({ Role.TEACHER, Role.ADMIN })
    @DeleteMapping("/{documentCode}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable String documentCode,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {

        Long userId = Long.valueOf(userIdStr);
        documentService.deleteDocument(documentCode, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Document deleted successfully");
        response.put("documentCode", documentCode);
        response.put("deletedAt", java.time.LocalDateTime.now());

        return ResponseEntity.ok(response);

    }
}