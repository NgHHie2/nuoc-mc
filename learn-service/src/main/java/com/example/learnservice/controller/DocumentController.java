package com.example.learnservice.controller;

import com.example.learnservice.model.Document;
import com.example.learnservice.util.FileUtil;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;

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

@RestController
@RequestMapping("/document")
public class DocumentController {
    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;

    @Autowired
    private FileUtil fileUtil;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadFile(
            @Parameter(description = "File to upload", content = @Content(mediaType = "multipart/form-data")) @RequestParam("file") MultipartFile file) {
        try {
            Document document = fileUtil.validateFile(file);

            // Mã hóa và lưu file
            String filePath = fileUtil.encryptFile(file, document.getName());
            // Lưu luôn không mã hóa
            // String filePath = fileUtil.uploadNotEncryptFile(file, document.getName());
            document.setFilePath(filePath);

            // Trả về thông tin file
            Map<String, Object> response = new HashMap<>();
            response.put("fileName", document.getName());
            response.put("filePath", document.getFilePath());
            response.put("fileSize", document.getSize());
            response.put("fileType", document.getFormat());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileName) {
        try {
            // Sanitize filename
            String sanitizedFileName = FilenameUtils.getName(fileName);
            Path filePath = Paths.get(uploadDir, sanitizedFileName);

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // Giải mã file
            byte[] decryptedContent = fileUtil.decryptFile(filePath.toFile());

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + sanitizedFileName + "\"")
                    .body(decryptedContent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
