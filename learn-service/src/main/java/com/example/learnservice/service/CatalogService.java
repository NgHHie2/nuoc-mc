package com.example.learnservice.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.learnservice.model.Catalog;
import com.example.learnservice.model.Document;
import com.example.learnservice.model.Position;
import com.example.learnservice.repository.CatalogRepository;
import com.example.learnservice.repository.DocumentRepository;
import com.example.learnservice.repository.PositionRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CatalogService {
    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Transactional
    public void updateCatalogs(Document document, List<Long> newPositionIds, Long userId) {
        // Lấy danh sách position IDs hiện tại
        Set<Long> currentPositionIds = document.getCatalogs().stream()
                .map(catalog -> catalog.getPosition().getId())
                .collect(Collectors.toSet());

        // Lọc và validate position IDs được yêu cầu
        Set<Long> requestedPositionIds = newPositionIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Nếu không có thay đổi thì return
        if (currentPositionIds.equals(requestedPositionIds)) {
            log.debug("No changes in positions for document: {}", document.getCode());
            return;
        }

        // Tìm positions cần xóa
        Set<Long> positionsToRemove = new HashSet<>(currentPositionIds);
        positionsToRemove.removeAll(requestedPositionIds);

        // Tìm positions cần thêm
        Set<Long> positionsToAdd = new HashSet<>(requestedPositionIds);
        positionsToAdd.removeAll(currentPositionIds);

        // Xóa catalogs không còn cần thiết
        if (!positionsToRemove.isEmpty()) {
            List<Catalog> catalogsToDelete = document.getCatalogs().stream()
                    .filter(catalog -> positionsToRemove.contains(catalog.getPosition().getId()))
                    .collect(Collectors.toList());

            catalogRepository.deleteAll(catalogsToDelete);
            document.getCatalogs().removeAll(catalogsToDelete);

            log.debug("Removed catalogs for positions: {}", positionsToRemove);
        }

        // Thêm catalogs mới
        if (!positionsToAdd.isEmpty()) {
            List<Catalog> newCatalogs = new ArrayList<>();

            for (Long positionId : positionsToAdd) {
                // Kiểm tra position có tồn tại không
                Position position = positionRepository.findById(positionId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Position not found with id: " + positionId));

                Catalog newCatalog = new Catalog();
                newCatalog.setPosition(position);
                newCatalog.setDocument(document);
                newCatalog.setCreatedBy(userId);
                newCatalog.setUpdatedBy(userId);
                newCatalogs.add(newCatalog);
            }

            List<Catalog> savedCatalogs = catalogRepository.saveAll(newCatalogs);
            document.getCatalogs().addAll(savedCatalogs);

            log.debug("Added catalogs for positions: {}", positionsToAdd);
        }

        log.info("Updated document {} catalogs: removed {}, added {}",
                document.getCode(), positionsToRemove.size(), positionsToAdd.size());
    }
}
