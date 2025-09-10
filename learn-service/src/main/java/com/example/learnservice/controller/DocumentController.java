package com.example.learnservice.controller;

import com.example.learnservice.model.Document;
import com.example.learnservice.service.DocumentService;
import com.example.learnservice.util.FileUtil;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
            String cccd = request.getHeader("X-CCCD"); // thông tin để watermark

            // Sanitize filename
            String sanitizedFileName = FilenameUtils.getName(fileName);
            Path filePath = Paths.get(uploadDir, "doc", sanitizedFileName);

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // Giải mã file gốc
            byte[] decryptedContent = fileUtil.decryptFile(filePath.toFile());

            // Thêm watermark
            byte[] watermarkedPdf = fileUtil.addWatermark(decryptedContent, cccd);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + sanitizedFileName + "\"")
                    .body(watermarkedPdf);
        } catch (Exception e) {
            log.error("Error download file", e);
            return ResponseEntity.badRequest().build();
        }
    }

}
