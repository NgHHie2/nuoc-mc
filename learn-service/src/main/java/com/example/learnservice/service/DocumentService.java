package com.example.learnservice.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.learnservice.model.Document;
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

    public Document saveDocument(Document document) {
        return documentRepository.save(document);
    }

    public Document processAndSaveDocument(MultipartFile file, Long userId) throws Exception {
        // Validate file
        Document document = fileUtil.validateFile(file);

        // Set user info
        document.setCreatedBy(userId);
        document.setUpdatedBy(userId);

        // Phân tích nội dung file để set pages/minutes
        fileUtil.analyzeFileContent(file, document);

        // Mã hóa và lưu file
        String filePath = fileUtil.encryptFile(file, document.getName());
        document.setFilePath(filePath);

        // Generate unique code for document
        document.setCode(UUID.randomUUID().toString());

        return documentRepository.save(document);
    }
}
