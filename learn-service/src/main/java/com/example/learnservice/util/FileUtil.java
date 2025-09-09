package com.example.learnservice.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.learnservice.enums.DocumentFormat;
import com.example.learnservice.model.Document;

@Component
public class FileUtil {

    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${file.encryption.key}")
    private String encryptionKey;

    private final Tika tika = new Tika();

    // Allowed MIME types
    private final Set<String> allowedMimeTypes = Set.of(
            "application/pdf",
            "video/mp4",
            "video/avi",
            "video/quicktime",
            "video/x-msvideo");

    public Document validateFile(MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Detect MIME type using Tika
        String detectedMimeType = tika.detect(file.getInputStream());
        if (!allowedMimeTypes.contains(detectedMimeType)) {
            throw new IllegalArgumentException("File type not allowed: " + detectedMimeType);
        }

        // Sanitize filename using Commons IO
        String originalFilename = file.getOriginalFilename();
        String sanitizedName = FilenameUtils.getName(originalFilename); // Chống path traversal

        if (sanitizedName == null || sanitizedName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        Document document = new Document();
        document.setName((sanitizedName));
        DocumentFormat format = switch (detectedMimeType) {
            case "application/pdf" -> DocumentFormat.PDF;
            case "video/mp4", "video/avi", "video/quicktime", "video/x-msvideo" -> DocumentFormat.VIDEO;
            default -> throw new IllegalArgumentException("File type not allowed: " + detectedMimeType);
        };
        document.setFormat(format);
        document.setSize(file.getSize());

        return document;
    }

    public String encryptFile(MultipartFile file, String originalFileName) throws Exception {
        // Tạo thư mục nếu chưa tồn tại
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        InputStream inputStream = file.getInputStream();
        String extension = FilenameUtils.getExtension(originalFileName);
        String fileName = UUID.randomUUID().toString() + "." + extension;
        Path filePath = Paths.get(uploadDir, fileName);
        File outputFile = filePath.toFile();

        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        try (FileOutputStream fos = new FileOutputStream(outputFile);
                CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {

            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
        }

        return filePath.toString();
    }

    public byte[] decryptFile(File inputFile) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        try (FileInputStream fis = new FileInputStream(inputFile);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] decrypted = cipher.update(buffer, 0, bytesRead);
                if (decrypted != null) {
                    baos.write(decrypted);
                }
            }
            byte[] finalDecrypted = cipher.doFinal();
            if (finalDecrypted != null) {
                baos.write(finalDecrypted);
            }
            return baos.toByteArray();
        }
    }

    public String uploadNotEncryptFile(MultipartFile file, String originalFileName) throws Exception {
        InputStream inputStream = file.getInputStream();
        String extension = FilenameUtils.getExtension(originalFileName);
        String fileName = UUID.randomUUID().toString() + "." + extension;
        Path filePath = Paths.get(uploadDir, fileName);
        File outputFile = filePath.toFile();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        return filePath.toString();
    }
}
