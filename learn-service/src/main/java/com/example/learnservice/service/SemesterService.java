// ================ UPDATED SEMESTER SERVICE ================

package com.example.learnservice.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.learnservice.dto.SemesterAccountRequest;
import com.example.learnservice.dto.SemesterCreateRequest;
import com.example.learnservice.dto.SemesterUpdateRequest;
import com.example.learnservice.dto.SemesterDetailDTO;
import com.example.learnservice.dto.SemesterDocumentRequest;
import com.example.learnservice.dto.SemesterSearchDTO;
import com.example.learnservice.model.Document;
import com.example.learnservice.model.Position;
import com.example.learnservice.model.Semester;
import com.example.learnservice.model.SemesterAccount;
import com.example.learnservice.model.SemesterDocument;
import com.example.learnservice.repository.DocumentRepository;
import com.example.learnservice.repository.PositionRepository;
import com.example.learnservice.repository.SemesterAccountRepository;
import com.example.learnservice.repository.SemesterDocumentRepository;
import com.example.learnservice.repository.SemesterRepository;
import com.example.learnservice.specification.SemesterSpecification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SemesterService {
    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private SemesterAccountRepository semesterAccountRepository;

    @Autowired
    private SemesterDocumentRepository semesterDocumentRepository;

    public List<Semester> getAllSemester() {
        return semesterRepository.findAll();
    }

    /**
     * Tìm kiếm semester với các tiêu chí đa dạng và phân trang
     */
    public Page<SemesterDetailDTO> universalSearch(SemesterSearchDTO searchDTO, Pageable pageable) {
        Sort sort = pageable.getSort().and(Sort.by(Sort.Direction.DESC, "createdAt"));
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Specification<Semester> spec = SemesterSpecification.build(searchDTO);

        Page<Semester> semesterPage = semesterRepository.findAll(spec, pageable);

        return semesterPage.map(semester -> {
            SemesterDetailDTO dto = new SemesterDetailDTO();
            dto.setId(semester.getId());
            dto.setName(semester.getName());
            dto.setStartDate(semester.getStartDate());
            dto.setEndDate(semester.getEndDate());
            dto.setCreatedAt(semester.getCreatedAt());
            dto.setCreatedBy(semester.getCreatedBy());

            // Tính tổng số accounts duy nhất trong semester
            Long totalAccounts = Optional.ofNullable(semester.getSemesterAccounts())
                    .orElse(List.of())
                    .stream()
                    .map(sa -> sa.getAccountId())
                    .distinct()
                    .count();
            dto.setTotalAccounts(totalAccounts);

            return dto;
        });
    }

    public Semester saveSemester(SemesterCreateRequest semesterCreateRequest, Long userId) {
        Semester semester = new Semester();
        semester.setName(semesterCreateRequest.getName());

        if (semesterCreateRequest.getEndDate().isBefore(semesterCreateRequest.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate is before startDate");
        }

        semester.setStartDate(semesterCreateRequest.getStartDate());
        semester.setEndDate(semesterCreateRequest.getEndDate());
        semester.setCreatedBy(userId);
        semester.setUpdatedBy(userId);

        return semesterRepository.save(semester);
    }

    @Transactional
    public Semester updateSemester(Long semesterId, SemesterUpdateRequest updateRequest, Long userId) {
        Optional<Semester> semesterOpt = semesterRepository.findById(semesterId);
        if (semesterOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Semester not found with id: " + semesterId);
        }

        Semester semester = semesterOpt.get();

        if (updateRequest.getEndDate().isBefore(updateRequest.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate is before startDate");
        }

        semester.setName(updateRequest.getName());
        semester.setStartDate(updateRequest.getStartDate());
        semester.setEndDate(updateRequest.getEndDate());
        semester.setUpdatedBy(userId);

        return semesterRepository.save(semester);
    }

    @Transactional
    public void deleteSemester(Long semesterId, Long userId) {
        Optional<Semester> semesterOpt = semesterRepository.findById(semesterId);
        if (semesterOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Semester not found with id: " + semesterId);
        }

        Semester semester = semesterOpt.get();

        // Kiểm tra có documents hoặc accounts không
        if ((semester.getSemesterDocuments() != null && !semester.getSemesterDocuments().isEmpty()) ||
                (semester.getSemesterAccounts() != null && !semester.getSemesterAccounts().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete semester with existing documents or accounts. Please remove all associations first.");
        }

        log.info("Deleting semester: {} (ID: {}) by user: {}",
                semester.getName(), semester.getId(), userId);

        semesterRepository.delete(semester);

        log.info("Semester deleted successfully: {}", semesterId);
    }

    public Semester getSemesterById(Long semesterId) {
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Semester not found with id: " + semesterId));
    }

    /**
     * Thêm documents vào semester (không xóa documents cũ)
     */
    @Transactional
    public void addDocumentsToSemester(Long semesterId, SemesterDocumentRequest request, Long userId) {
        Semester semester = getSemesterById(semesterId);

        // Get current document codes
        Set<String> currentCodes = semester.getSemesterDocuments().stream()
                .map(sd -> sd.getDocument().getCode())
                .collect(Collectors.toSet());

        // Filter out documents that already exist
        List<String> newDocumentCodes = request.getDocumentCodes().stream()
                .filter(code -> !currentCodes.contains(code))
                .collect(Collectors.toList());

        if (newDocumentCodes.isEmpty()) {
            log.info("No new documents to add to semester {}", semesterId);
            return;
        }

        // Get all documents in one query
        List<Document> foundDocuments = documentRepository.findAllByCodeIn(newDocumentCodes);

        // Check if all documents were found
        Set<String> foundCodes = foundDocuments.stream()
                .map(Document::getCode)
                .collect(Collectors.toSet());

        List<String> notFoundCodes = newDocumentCodes.stream()
                .filter(code -> !foundCodes.contains(code))
                .collect(Collectors.toList());

        if (!notFoundCodes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Documents not found with codes: " + String.join(", ", notFoundCodes));
        }

        // Add new documents
        List<SemesterDocument> newSemesterDocuments = foundDocuments.stream()
                .map(doc -> {
                    SemesterDocument sd = new SemesterDocument();
                    sd.setDocument(doc);
                    sd.setSemester(semester);
                    sd.setCreatedBy(userId);
                    return sd;
                })
                .collect(Collectors.toList());

        List<SemesterDocument> saved = semesterDocumentRepository.saveAll(newSemesterDocuments);
        semester.getSemesterDocuments().addAll(saved);

        log.info("Added {} new documents to semester {}", foundDocuments.size(), semesterId);
    }

    /**
     * Xóa document khỏi semester
     */
    @Transactional
    public void removeDocumentFromSemester(Long semesterId, String documentCode, Long userId) {
        Semester semester = getSemesterById(semesterId);

        SemesterDocument toRemove = semester.getSemesterDocuments().stream()
                .filter(sd -> sd.getDocument().getCode().equals(documentCode))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Document not found in semester"));

        semesterDocumentRepository.delete(toRemove);
        semester.getSemesterDocuments().remove(toRemove);

        log.info("Removed document {} from semester {} by user {}", documentCode, semesterId, userId);
    }

    /**
     * Thêm accounts vào semester (không xóa cũ)
     */
    @Transactional
    public void addAccountsToSemester(Long semesterId, SemesterAccountRequest request, Long userId) {
        Semester semester = getSemesterById(semesterId);

        // Get current account-position pairs
        Set<String> currentPairs = semester.getSemesterAccounts().stream()
                .map(sa -> sa.getAccountId() + "_" + sa.getPosition().getId())
                .collect(Collectors.toSet());

        // Filter new assignments
        List<SemesterAccountRequest.AccountPositionAssignment> newAssignments = request.getAccountAssignments().stream()
                .filter(assignment -> !currentPairs.contains(
                        assignment.getAccountId() + "_" + assignment.getPositionId()))
                .collect(Collectors.toList());

        if (newAssignments.isEmpty())
            return;

        // Get positions in one query
        List<Long> positionIds = newAssignments.stream()
                .map(SemesterAccountRequest.AccountPositionAssignment::getPositionId)
                .distinct().collect(Collectors.toList());

        Map<Long, Position> positionMap = positionRepository.findAllById(positionIds).stream()
                .collect(Collectors.toMap(Position::getId, p -> p));

        // Create new assignments
        List<SemesterAccount> newSemesterAccounts = newAssignments.stream()
                .map(assignment -> {
                    SemesterAccount sa = new SemesterAccount();
                    sa.setSemester(semester);
                    sa.setAccountId(assignment.getAccountId());
                    sa.setPosition(positionMap.get(assignment.getPositionId()));
                    sa.setCreatedBy(userId);
                    return sa;
                }).collect(Collectors.toList());

        semesterAccountRepository.saveAll(newSemesterAccounts);
        log.info("Added {} accounts to semester {}", newAssignments.size(), semesterId);
    }

    /**
     * Xóa account khỏi semester
     */
    @Transactional
    public void removeAccountFromSemester(Long semesterId, Long accountId, Long userId) {
        Semester semester = getSemesterById(semesterId);

        List<SemesterAccount> toRemove = semester.getSemesterAccounts().stream()
                .filter(sa -> sa.getAccountId().equals(accountId))
                .collect(Collectors.toList());

        if (toRemove.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found in semester");
        }

        semesterAccountRepository.deleteAll(toRemove);
        log.info("Removed account {} from semester {}", accountId, semesterId);
    }

    /**
     * Sửa position của account trong semester
     */
    @Transactional
    public void updateAccountPosition(Long semesterId, Long accountId, Long newPositionId, Long userId) {
        Semester semester = getSemesterById(semesterId);

        SemesterAccount semesterAccount = semester.getSemesterAccounts().stream()
                .filter(sa -> sa.getAccountId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account not found in semester"));

        Position newPosition = positionRepository.findById(newPositionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Position not found"));

        semesterAccount.setPosition(newPosition);
        semesterAccountRepository.save(semesterAccount);

        log.info("Updated account {} position in semester {}", accountId, semesterId);
    }

    /**
     * Lấy danh sách documents trong semester
     */
    public List<String> getDocumentsInSemester(Long semesterId) {
        Semester semester = getSemesterById(semesterId);
        return semester.getSemesterDocuments().stream()
                .map(sd -> sd.getDocument().getCode())
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách accounts với positions trong semester
     */
    public List<Map<String, Object>> getAccountsInSemester(Long semesterId) {
        Semester semester = getSemesterById(semesterId);
        return semester.getSemesterAccounts().stream()
                .map(sa -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("accountId", sa.getAccountId());
                    info.put("positionId", sa.getPosition().getId());
                    info.put("positionName", sa.getPosition().getName());
                    info.put("createdAt", sa.getCreatedAt());
                    return info;
                })
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra account có quyền truy cập document thông qua semester không
     */
    public boolean checkDocumentAccessThroughSemester(Long semesterId, Long accountId, String documentCode) {
        return semesterAccountRepository.existsAccessThroughPosition(semesterId, accountId, documentCode);
    }
}