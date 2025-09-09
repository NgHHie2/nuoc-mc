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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.XMPDM;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.learnservice.enums.DocumentFormat;
import com.example.learnservice.model.Document;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        document.setName(sanitizedName);
        DocumentFormat format = switch (detectedMimeType) {
            case "application/pdf" -> DocumentFormat.PDF;
            case "video/mp4", "video/avi", "video/quicktime", "video/x-msvideo" -> DocumentFormat.VIDEO;
            default -> throw new IllegalArgumentException("File type not allowed: " + detectedMimeType);
        };
        document.setFormat(format);
        document.setSize(file.getSize());

        return document;
    }

    public void analyzeFileContent(MultipartFile file, Document document) {
        try {
            if (document.getFormat() == DocumentFormat.PDF) {
                // Phân tích PDF để lấy số trang
                int pageCount = analyzePdfPages(file);
                document.setPages(pageCount);
                log.info("PDF analysis completed - Pages: {}", pageCount);
            } else if (document.getFormat() == DocumentFormat.VIDEO) {
                // Phân tích video để lấy duration (phút)
                Integer durationMinutes = analyzeVideoDuration(file);
                document.setMinutes(durationMinutes);
                log.info("Video analysis completed - Duration: {} minutes", durationMinutes);
            }
        } catch (Exception e) {
            log.warn("Failed to analyze file content for {}: {}", document.getName(), e.getMessage());
            // Không throw exception để không fail upload, chỉ log warning
        }
    }

    private int analyzePdfPages(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
                PDDocument document = PDDocument.load(inputStream)) {
            return document.getNumberOfPages();
        } catch (Exception e) {
            log.error("Failed to analyze PDF pages: {}", e.getMessage());
            return 0; // Return 0 if analysis fails
        }
    }

    private Integer analyzeVideoDuration(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1); // No limit
            Metadata metadata = new Metadata();
            ParseContext parseContext = new ParseContext();

            parser.parse(inputStream, handler, metadata, parseContext);

            // Try to get duration from metadata
            String duration = metadata.get(XMPDM.DURATION);
            if (duration != null) {
                try {
                    // Duration might be in seconds, convert to minutes
                    double durationSeconds = Double.parseDouble(duration);
                    return (int) Math.ceil(durationSeconds / 60.0);
                } catch (NumberFormatException e) {
                    log.warn("Could not parse duration: {}", duration);
                }
            }

            // Alternative metadata fields for duration
            String[] durationFields = {
                    "duration", "Duration", "DURATION",
                    "xmpDM:duration", "Content-Duration"
            };

            for (String field : durationFields) {
                String value = metadata.get(field);
                if (value != null) {
                    try {
                        double seconds = Double.parseDouble(value);
                        return (int) Math.ceil(seconds / 60.0);
                    } catch (NumberFormatException e) {
                        // Try next field
                        continue;
                    }
                }
            }

            log.warn("Could not extract video duration from metadata");
            return null;

        } catch (Exception e) {
            log.error("Failed to analyze video duration: {}", e.getMessage());
            return null;
        }
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