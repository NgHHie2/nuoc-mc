package com.example.learnservice.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.example.learnservice.dto.DocumentSearchDTO;
import com.example.learnservice.enums.DocumentFormat;
import com.example.learnservice.model.Document;
import com.example.learnservice.model.Catalog;
import com.example.learnservice.model.Tag;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Component
public class DocumentSpecification {

    public static Specification<Document> hasFormat(DocumentFormat format) {
        return (root, query, cb) -> format == null ? cb.conjunction() : cb.equal(root.get("format"), format);
    }

    public static Specification<Document> hasCatalogs(List<Long> catalogIds) {
        return (root, query, cb) -> {
            if (catalogIds == null || catalogIds.isEmpty()) {
                return cb.conjunction();
            }
            Join<Document, Catalog> catalogJoin = root.join("catalogs", JoinType.LEFT);
            return catalogJoin.get("positionId").in(catalogIds);
        };
    }

    public static Specification<Document> keywordInFields(String keyword, List<String> searchFields) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String likePattern = "%" + keyword.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            List<String> fieldsToSearch = (searchFields == null || searchFields.isEmpty())
                    ? List.of("name", "documentNumber", "tags")
                    : searchFields;

            for (String field : fieldsToSearch) {
                switch (field) {
                    case "name":
                        predicates.add(cb.like(cb.lower(root.get("name")), likePattern));
                        break;
                    case "documentNumber":
                        predicates.add(cb.and(
                                cb.isNotNull(root.get("documentNumber")),
                                cb.like(cb.lower(root.get("documentNumber")), likePattern)));
                        break;
                    case "tags":
                        Join<Document, Tag> tagJoin = root.join("tags", JoinType.LEFT);
                        predicates.add(cb.like(cb.lower(tagJoin.get("name")), likePattern));
                        break;
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Document> build(DocumentSearchDTO searchDTO) {
        return Specification.where(hasFormat(searchDTO.getFormat()))
                .and(hasCatalogs(searchDTO.getCatalogIds()))
                .and(keywordInFields(searchDTO.getKeyword(), searchDTO.getSearchFields()));
    }
}