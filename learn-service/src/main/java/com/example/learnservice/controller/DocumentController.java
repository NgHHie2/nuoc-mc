package com.example.learnservice.controller;

import com.example.learnservice.dto.DocumentUploadRequest;
import com.example.learnservice.enums.DocumentFormat;
import com.example.learnservice.model.Document;
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
    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;

    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

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
            response.put("filePath", savedDocument.getFilePath());
            response.put("fileSize", savedDocument.getSize());
            response.put("fileType", savedDocument.getFormat());
            response.put("pages", savedDocument.getPages());
            response.put("minutes", savedDocument.getMinutes());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/download/{fileCode}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileCode,
            HttpServletRequest request) throws Exception {

        log.info("X-CCCD: " + request.getHeader("X-CCCD"));
        String cccd = request.getHeader("X-CCCD");

        System.out.println(request.getHeader("X-Positions"));
        List<Long> positions = Arrays.stream(request.getHeader("X-Positions").split(","))
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .toList();

        Document document = documentService.getDocumentByCode(fileCode);
        if (!documentService.checkDocumentAccess(document, positions)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission denied");
        }
        Path filePath = documentService.getDocumentPath(document);

        // Thêm watermark dựa theo format
        byte[] watermarkedContent = documentService.getFileContent(filePath, document, cccd);

        String encodedFileName = URLEncoder.encode(document.getName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        String contentDisposition = "attachment; filename*=UTF-8''" + encodedFileName;

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
    @GetMapping("/stream/{fileName}")
    public ResponseEntity<ResourceRegion> streamVideo(
            @PathVariable String fileName,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws Exception {

        String cccd = request.getHeader("X-CCCD");
        if (cccd == null || cccd.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String sanitizedFileName = FilenameUtils.getName(fileName);
        Path filePath = Paths.get(uploadDir, "doc", sanitizedFileName);

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        String documentCode = FilenameUtils.getBaseName(sanitizedFileName);
        Optional<Document> documentOpt = documentRepository.findByCode(documentCode);
        if (documentOpt.isEmpty() || documentOpt.get().getFormat() != DocumentFormat.VIDEO) {
            return ResponseEntity.notFound().build();
        }

        // Tạo cached watermarked file nếu chưa có
        String cacheKey = documentCode + "_" + cccd;
        File cachedFile = new File("D:/temp/cache_" + cacheKey + ".mp4");
        if (!cachedFile.exists()) {
            byte[] decryptedContent = fileUtil.decryptFile(filePath.toFile());
            byte[] watermarkedContent = fileUtil.addVideoWatermark(decryptedContent, cccd);
            Files.write(cachedFile.toPath(), watermarkedContent);
            log.info("Created and cached watermarked video for {}", documentCode);
        }

        FileSystemResource videoResource = new FileSystemResource(cachedFile);
        long contentLength = videoResource.contentLength();

        // Xử lý Range header (nếu có)
        ResourceRegion region;
        if (headers.getRange() != null && !headers.getRange().isEmpty()) {
            HttpRange range = headers.getRange().get(0);
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(1 * 1024 * 1024, end - start + 1); // chunk ~1MB
            region = new ResourceRegion(videoResource, start, rangeLength);
        } else {
            long rangeLength = Math.min(1 * 1024 * 1024, contentLength);
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
     */
    @GetMapping("/preview/{documentCode}")
    public ResponseEntity<byte[]> getPreview(@PathVariable String documentCode) {
        try {
            Optional<Document> documentOpt = documentRepository.findByCode(documentCode);
            if (documentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Document document = documentOpt.get();
            if (document.getPreviewPath() == null) {
                return ResponseEntity.notFound().build();
            }
            Path previewPath = Paths.get(uploadDir, "preview", document.getCode() + ".jpg");
            byte[] previewBytes = fileUtil.getPreviewImage(previewPath);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(previewBytes);

        } catch (Exception e) {
            log.error("Error getting preview", e);
            return ResponseEntity.notFound().build();
        }
    }
}