package com.example.learnservice.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.learnservice.dto.DocumentSearchDTO;
import com.example.learnservice.dto.DocumentUpdateRequest;
import com.example.learnservice.dto.DocumentUploadRequest;
import com.example.learnservice.enums.DocumentFormat;
import com.example.learnservice.model.Catalog;
import com.example.learnservice.model.Document;
import com.example.learnservice.model.Position;
import com.example.learnservice.model.Tag;
import com.example.learnservice.repository.CatalogRepository;
import com.example.learnservice.repository.DocumentRepository;
import com.example.learnservice.repository.PositionRepository;
import com.example.learnservice.repository.TagRepository;
import com.example.learnservice.specification.DocumentSpecification;
import com.example.learnservice.util.FileUtil;
import com.example.learnservice.util.listener.event.DocumentCreatedEvent;
import com.example.learnservice.util.listener.event.DocumentDeletedEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DocumentService {
    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;

    public Document saveDocument(Document document) {
        return documentRepository.save(document);
    }

    @Transactional
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
        // String filePath = fileUtil.encryptFile(file, document);
        String filePath = fileUtil.uploadNotEncryptFile(file, document);
        // document.setFilePath(filePath);

        // Tạo preview image
        String previewPath = fileUtil.generatePreview(file, document);
        // document.setPreviewPath(previewPath);

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
        if (request.getPositions() != null && !request.getPositions().isEmpty()) {
            List<Catalog> catalogs = new ArrayList<>();
            for (Long positionId : request.getPositions()) {
                if (positionId != null) {
                    Position position = positionRepository.findById(positionId).orElse(null);
                    if (position != null) {
                        Catalog catalog = new Catalog();
                        catalog.setPosition(position);
                        catalog.setDocument(savedDocument);
                        catalog.setCreatedBy(userId);
                        catalog.setUpdatedBy(userId);
                        catalogs.add(catalog);
                    }
                }
            }
            savedDocument.setCatalogs(catalogs);
        }

        applicationEventPublisher.publishEvent(new DocumentCreatedEvent(savedDocument));
        // Lưu lại để persist tags và catalogs
        return documentRepository.save(savedDocument);
    }

    public Document getDocumentByCode(String code) {
        Optional<Document> documentOpt = documentRepository.findByCode(code);
        if (documentOpt.isEmpty())
            return null;
        return documentOpt.get();
    }

    public Document getDocumentByDocumentNumber(String documentNumber) {
        Optional<Document> documentOpt = documentRepository.findByDocumentNumber(documentNumber);
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

    public Path getPreviewPath(Document document) {
        String fileName = document.getCode() + ".jpg";
        Path filePath = Paths.get(uploadDir, "preview", fileName);
        if (!Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        return filePath;
    }

    public byte[] getFileContent(Path filePath, Document document, String cccd) throws Exception {
        // Giải mã file gốc
        // byte[] decryptedContent = fileUtil.decryptFile(filePath.toFile());
        byte[] decryptedContent = fileUtil.getFileContent(filePath.toFile());

        // Thêm watermark dựa theo format
        byte[] watermarkedContent = null;

        if (document.getFormat() == DocumentFormat.PDF) {
            watermarkedContent = fileUtil.addWatermark(decryptedContent, cccd);
        }
        // else if (document.getFormat() == DocumentFormat.VIDEO) {
        // watermarkedContent = fileUtil.addVideoWatermark(decryptedContent, cccd);
        // }

        return watermarkedContent;
    }

    /**
     * Tìm kiếm tài liệu với các tiêu chí đa dạng và phân trang
     * 
     * @param searchDTO - Chứa các tiêu chí tìm kiếm
     * @param pageable  - Thông tin phân trang
     * @return Page<Document> - Kết quả phân trang
     */
    public Page<Document> universalSearch(DocumentSearchDTO searchDTO, Pageable pageable) {
        Sort sort = pageable.getSort().and(Sort.by(Sort.Direction.DESC, "createdAt"));
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Specification<Document> spec = DocumentSpecification.build(searchDTO);
        return documentRepository.findAll(spec, pageable);
    }

    /**
     * Cập nhật thông tin tài liệu
     */
    @Transactional
    public Document updateDocument(String documentCode, DocumentUpdateRequest request, Long userId) {
        // Tìm document theo code
        Optional<Document> documentOpt = documentRepository.findByCode(documentCode);
        if (documentOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found with code: " + documentCode);
        }

        Document document = documentOpt.get();

        // Cập nhật các trường thông tin cơ bản
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            document.setName(request.getName().trim());
        }

        if (request.getDocumentNumber() != null) {
            document.setDocumentNumber(request.getDocumentNumber().trim());
        }

        if (request.getDescription() != null) {
            document.setDescription(request.getDescription().trim());
        }

        // Cập nhật user thay đổi
        document.setUpdatedBy(userId);

        // Cập nhật Tags - chỉ cập nhật khi có thay đổi
        if (request.getTags() != null) {
            updateTags(document, request.getTags());
        }

        // Cập nhật Catalogs - chỉ cập nhật khi có thay đổi
        // if (request.getCatalogs() != null) {
        // updateCatalogs(document, request.getCatalogs(), userId);
        // }

        return documentRepository.save(document);
    }

    private void updateTags(Document document, List<String> newTagNames) {
        Set<String> currentTagNames = document.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        Set<String> requestedTagNames = newTagNames.stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toSet());

        if (currentTagNames.equals(requestedTagNames)) {
            return;
        }

        Set<String> tagsToRemove = new HashSet<>(currentTagNames);
        tagsToRemove.removeAll(requestedTagNames);

        Set<String> tagsToAdd = new HashSet<>(requestedTagNames);
        tagsToAdd.removeAll(currentTagNames);

        if (!tagsToRemove.isEmpty()) {
            List<Tag> tagsToDelete = document.getTags().stream()
                    .filter(tag -> tagsToRemove.contains(tag.getName()))
                    .collect(Collectors.toList());

            tagRepository.deleteAll(tagsToDelete);
            document.getTags().removeAll(tagsToDelete);

            log.debug("Removed tags: {}", tagsToRemove);
        }

        if (!tagsToAdd.isEmpty()) {
            List<Tag> newTags = new ArrayList<>();
            for (String tagName : tagsToAdd) {
                Tag newTag = new Tag();
                newTag.setName(tagName);
                newTag.setDocument(document);
                newTags.add(newTag);
            }

            List<Tag> savedTags = tagRepository.saveAll(newTags);
            document.getTags().addAll(savedTags);

            log.debug("Added tags: {}", tagsToAdd);
        }
    }

    /**
     * Xóa tài liệu
     */
    @Transactional
    public void deleteDocument(String documentCode, Long userId) {
        Optional<Document> documentOpt = documentRepository.findByCode(documentCode);
        if (documentOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found with code: " + documentCode);
        }

        Document document = documentOpt.get();

        // Log thông tin trước khi xóa
        log.info("Deleting document: {} (Code: {}) by user: {}",
                document.getName(), document.getCode(), userId);

        // Xóa document (cascade sẽ tự động xóa tags và catalogs)
        documentRepository.delete(document);
        log.info("Document deleted successfully: {}", documentCode);
        applicationEventPublisher.publishEvent(new DocumentDeletedEvent(document));
    }

}
