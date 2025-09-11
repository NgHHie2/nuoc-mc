package com.example.learnservice.controller;

import com.example.learnservice.enums.DocumentFormat;
import com.example.learnservice.model.Document;
import com.example.learnservice.repository.DocumentRepository;
import com.example.learnservice.service.DocumentService;
import com.example.learnservice.util.FileUtil;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            @Parameter(description = "File to upload", content = @Content(mediaType = "multipart/form-data")) @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            // Lấy User ID từ header
            String userIdHeader = request.getHeader("X-User-Id");
            Long userId = Long.valueOf(userIdHeader);

            Document savedDocument = documentService.processAndSaveDocument(file, userId);

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

    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String fileName,
            HttpServletRequest request) {
        try {
            log.info("X-CCCD: " + request.getHeader("X-CCCD"));
            String cccd = request.getHeader("X-CCCD");

            // Sanitize filename
            String sanitizedFileName = FilenameUtils.getName(fileName);
            Path filePath = Paths.get(uploadDir, "doc", sanitizedFileName);

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // Lấy thông tin document từ DB để biết format
            String documentCode = FilenameUtils.getBaseName(sanitizedFileName);
            Optional<Document> documentOpt = documentRepository.findByCode(documentCode);

            if (documentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Document document = documentOpt.get();

            // Giải mã file gốc
            byte[] decryptedContent = fileUtil.decryptFile(filePath.toFile());

            // Thêm watermark dựa theo format
            byte[] watermarkedContent;
            String contentType;

            if (document.getFormat() == DocumentFormat.PDF) {
                watermarkedContent = fileUtil.addWatermark(decryptedContent, cccd);
                contentType = "application/pdf";
            } else if (document.getFormat() == DocumentFormat.VIDEO) {
                watermarkedContent = fileUtil.addVideoWatermark(decryptedContent, cccd);
                contentType = "video/mp4";
            } else {
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header("Content-Disposition", "attachment; filename=\"" + sanitizedFileName + "\"")
                    .body(watermarkedContent);

        } catch (Exception e) {
            log.error("Error download file", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Stream video với hỗ trợ HTTP Range requests
     */
    @GetMapping("/stream/{fileName}")
    public void streamVideo(
            @PathVariable String fileName,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        try {
            String cccd = request.getHeader("X-CCCD");
            if (cccd == null || cccd.isEmpty()) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return;
            }

            // Sanitize filename
            String sanitizedFileName = FilenameUtils.getName(fileName);
            Path filePath = Paths.get(uploadDir, "doc", sanitizedFileName);

            if (!Files.exists(filePath)) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return;
            }

            // Lấy thông tin document từ DB
            String documentCode = FilenameUtils.getBaseName(sanitizedFileName);
            Optional<Document> documentOpt = documentRepository.findByCode(documentCode);

            if (documentOpt.isEmpty() || documentOpt.get().getFormat() != DocumentFormat.VIDEO) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return;
            }

            // Giải mã và thêm watermark cho video
            String cacheKey = documentCode + "_" + cccd;
            File cachedFile = new File("D:/temp/cache_" + cacheKey + ".mp4");

            byte[] watermarkedContent;
            if (cachedFile.exists()) {
                // Dùng cache
                watermarkedContent = Files.readAllBytes(cachedFile.toPath());
                log.info("Using cached watermarked video");
            } else {
                // Tạo mới và cache
                byte[] decryptedContent = fileUtil.decryptFile(filePath.toFile());
                watermarkedContent = fileUtil.addVideoWatermark(decryptedContent, cccd);
                Files.write(cachedFile.toPath(), watermarkedContent);
                log.info("Created and cached watermarked video");
            }

            // Handle Range requests
            String rangeHeader = request.getHeader("Range");
            log.info("Range: " + rangeHeader);
            if (rangeHeader == null) {
                // No range, send entire file
                response.setStatus(HttpStatus.OK.value());
                response.setContentType("video/mp4");
                response.setContentLength(watermarkedContent.length);
                response.setHeader("Accept-Ranges", "bytes");

                try (OutputStream out = response.getOutputStream()) {
                    out.write(watermarkedContent);
                }
            } else {
                // Handle range request
                handleRangeRequest(watermarkedContent, rangeHeader, response);
            }

        } catch (Exception e) {
            log.error("Error streaming video", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    /**
     * Xử lý HTTP Range requests cho video streaming
     */
    private void handleRangeRequest(byte[] content, String rangeHeader, HttpServletResponse response)
            throws IOException {
        long fileLength = content.length;

        // Parse Range header: bytes=start-end
        Pattern rangePattern = Pattern.compile("bytes=([0-9]*)-([0-9]*)");
        Matcher matcher = rangePattern.matcher(rangeHeader);

        if (!matcher.matches()) {
            response.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
            return;
        }

        String startStr = matcher.group(1);
        String endStr = matcher.group(2);

        long start = 0;
        long end = fileLength - 1;

        if (!startStr.isEmpty()) {
            start = Long.parseLong(startStr);
        }

        if (!endStr.isEmpty()) {
            end = Long.parseLong(endStr);
        }

        // Validate range
        if (start >= fileLength || end >= fileLength || start > end) {
            response.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
            response.setHeader("Content-Range", "bytes */" + fileLength);
            return;
        }

        long contentLength = end - start + 1;

        // Set response headers for partial content
        response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
        response.setContentType("video/mp4");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        response.setContentLengthLong(contentLength);

        // Write partial content
        try (OutputStream out = response.getOutputStream()) {
            ByteArrayInputStream bis = new ByteArrayInputStream(content);
            bis.skip(start);

            byte[] buffer = new byte[8192];
            long bytesWritten = 0;
            int bytesRead;

            while (bytesWritten < contentLength && (bytesRead = bis.read(buffer)) != -1) {
                int toWrite = (int) Math.min(bytesRead, contentLength - bytesWritten);
                out.write(buffer, 0, toWrite);
                bytesWritten += toWrite;
            }
        }
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

            byte[] previewBytes = fileUtil.getPreviewImage(document.getPreviewPath());

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(previewBytes);

        } catch (Exception e) {
            log.error("Error getting preview", e);
            return ResponseEntity.notFound().build();
        }
    }
}