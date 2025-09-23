package com.example.learnservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.learnservice.dto.CatalogUpdateRequest;
import com.example.learnservice.model.Catalog;
import com.example.learnservice.model.Document;
import com.example.learnservice.model.Tag;
import com.example.learnservice.service.CatalogService;
import com.example.learnservice.service.DocumentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/document")
public class CatalogController {
    @Autowired
    private CatalogService catalogService;

    @Autowired
    private DocumentService documentService;

    /**
     * Cập nhật thông tin tài liệu
     */
    @PutMapping("/catalog/{documentCode}")
    public ResponseEntity<?> updateCatalogsDocument(
            @PathVariable String documentCode,
            @Valid @RequestBody CatalogUpdateRequest updateRequest,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long userId = Long.valueOf(userIdStr);
        List<Long> newPositionIds = updateRequest.getCatalogs();
        Document document = documentService.getDocumentByCode(documentCode);
        Document updatedDocument = null;
        if (document != null) {
            catalogService.updateCatalogs(document, newPositionIds, userId);
            updatedDocument = documentService.getDocumentByCode(documentCode);
        }

        Map<String, Object> response = new HashMap<>();

        // Catalogs
        if (updatedDocument != null && updatedDocument.getCatalogs() != null) {
            List<Long> positionIds = updatedDocument.getCatalogs().stream()
                    .map(Catalog::getPositionId)
                    .toList();
            System.out.println(documentCode);
            System.out.println(positionIds);
            response.put("catalogs", positionIds);
        }

        return ResponseEntity.ok(response);

    }
}
