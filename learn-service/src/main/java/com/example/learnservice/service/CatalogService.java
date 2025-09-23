package com.example.learnservice.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.learnservice.model.Catalog;
import com.example.learnservice.model.Document;
import com.example.learnservice.repository.CatalogRepository;
import com.example.learnservice.repository.DocumentRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CatalogService {
    @Autowired
    private CatalogRepository catalogRepository;

    @Transactional
    public void updateCatalogs(Document document, List<Long> newPositionIds, Long userId) {
        Set<Long> currentPositionIds = document.getCatalogs().stream()
                .map(Catalog::getPositionId)
                .collect(Collectors.toSet());

        Set<Long> requestedPositionIds = newPositionIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (currentPositionIds.equals(requestedPositionIds)) {
            return;
        }

        Set<Long> catalogsToRemove = new HashSet<>(currentPositionIds);
        catalogsToRemove.removeAll(requestedPositionIds);

        Set<Long> catalogsToAdd = new HashSet<>(requestedPositionIds);
        catalogsToAdd.removeAll(currentPositionIds);

        if (!catalogsToRemove.isEmpty()) {
            List<Catalog> catalogsToDelete = document.getCatalogs().stream()
                    .filter(catalog -> catalogsToRemove.contains(catalog.getPositionId()))
                    .collect(Collectors.toList());

            catalogRepository.deleteAll(catalogsToDelete);
            document.getCatalogs().removeAll(catalogsToDelete);

            log.debug("Removed catalogs: {}", catalogsToRemove);
        }

        if (!catalogsToAdd.isEmpty()) {
            List<Catalog> newCatalogs = new ArrayList<>();
            for (Long positionId : catalogsToAdd) {
                Catalog newCatalog = new Catalog();
                newCatalog.setPositionId(positionId);
                newCatalog.setDocument(document);
                newCatalog.setCreatedBy(userId);
                newCatalog.setUpdatedBy(userId);
                newCatalogs.add(newCatalog);
            }

            List<Catalog> savedCatalogs = catalogRepository.saveAll(newCatalogs);
            document.getCatalogs().addAll(savedCatalogs);

            log.debug("Added catalogs: {}", catalogsToAdd);
        }

    }
}
