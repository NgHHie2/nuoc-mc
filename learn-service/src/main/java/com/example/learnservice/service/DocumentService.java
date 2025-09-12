package com.example.learnservice.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.learnservice.dto.DocumentUploadRequest;
import com.example.learnservice.enums.DocumentFormat;
import com.example.learnservice.model.Catalog;
import com.example.learnservice.model.Document;
import com.example.learnservice.model.Tag;
import com.example.learnservice.repository.DocumentRepository;
import com.example.learnservice.util.FileUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DocumentService {
    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private FileUtil fileUtil;

    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;

    public Document saveDocument(Document document) {
        return documentRepository.save(document);
    }

    public Document processAndSaveDocument(MultipartFile file, Long userId, DocumentUploadRequest request)
            throws Exception {
        // Validate file
        Document document = fileUtil.validateFile(file);

        // Set user info
        document.setCreatedBy(userId);
        document.setUpdatedBy(userId);

        // Set thông tin từ DTO
        document.setDocumentNumber(request.getDocumentNumber());
        document.setDescription(request.getDescription());

        // Generate unique code for document (UUID - hệ thống tự sinh)
        String documentCode = UUID.randomUUID().toString();
        document.setCode(documentCode);

        // Phân tích nội dung file để set pages/minutes
        fileUtil.analyzeFileContent(file, document);

        // Mã hóa và lưu file với tên là documentCode
        String filePath = fileUtil.encryptFile(file, document);
        document.setFilePath(filePath);

        // Tạo preview image
        String previewPath = fileUtil.generatePreview(file, document);
        document.setPreviewPath(previewPath);

        // Lưu document trước để có ID
        Document savedDocument = documentRepository.save(document);

        // Tạo Tags
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            List<Tag> tags = new ArrayList<>();
            for (String tagName : request.getTags()) {
                if (tagName != null && !tagName.trim().isEmpty()) {
                    Tag tag = new Tag();
                    tag.setName(tagName.trim());
                    tag.setDocument(savedDocument);
                    tags.add(tag);
                }
            }
            savedDocument.setTags(tags);
        }

        // Tạo Catalogs
        if (request.getCatalogs() != null && !request.getCatalogs().isEmpty()) {
            List<Catalog> catalogs = new ArrayList<>();
            for (Long positionId : request.getCatalogs()) {
                if (positionId != null) {
                    Catalog catalog = new Catalog();
                    catalog.setPositionId(positionId);
                    catalog.setDocument(savedDocument);
                    catalog.setCreatedBy(userId);
                    catalog.setUpdatedBy(userId);
                    catalogs.add(catalog);
                }
            }
            savedDocument.setCatalogs(catalogs);
        }

        // Lưu lại để persist tags và catalogs
        return documentRepository.save(savedDocument);
    }

    public Document getDocumentByCode(String code) {
        Optional<Document> documentOpt = documentRepository.findByCode(code);
        if (documentOpt.isEmpty())
            return null;
        return documentOpt.get();
    }

    public Path getDocumentPath(Document document) {
        String fileName = document.getCode() + "." + FilenameUtils.getExtension(document.getName());
        // Sanitize filename
        String sanitizedFileName = FilenameUtils.getName(fileName);
        Path filePath = Paths.get(uploadDir, "doc", sanitizedFileName);

        if (!Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        return filePath;
    }

    public boolean checkDocumentAccess(Document document, List<Long> positions) {
        List<Long> required = document.getCatalogs().stream().map(Catalog::getPositionId).toList();
        if (Collections.disjoint(positions, required))
            return false;
        return true;
    }

    public byte[] getFileContent(Path filePath, Document document, String cccd) throws Exception {
        // Giải mã file gốc
        byte[] decryptedContent = fileUtil.decryptFile(filePath.toFile());

        // Thêm watermark dựa theo format
        byte[] watermarkedContent = null;

        if (document.getFormat() == DocumentFormat.PDF) {
            watermarkedContent = fileUtil.addWatermark(decryptedContent, cccd);
        } else if (document.getFormat() == DocumentFormat.VIDEO) {
            watermarkedContent = fileUtil.addVideoWatermark(decryptedContent, cccd);
        }

        return watermarkedContent;
    }
}
