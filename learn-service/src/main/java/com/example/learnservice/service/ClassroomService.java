package com.example.learnservice.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.learnservice.dto.ClassroomAccountRequest;
import com.example.learnservice.dto.ClassroomCreateRequest;
import com.example.learnservice.dto.ClassroomDocumentRequest;
import com.example.learnservice.dto.ClassroomUpdateRequest;
import com.example.learnservice.model.Classroom;
import com.example.learnservice.model.ClassroomAccount;
import com.example.learnservice.model.ClassroomDocument;
import com.example.learnservice.model.Document;
import com.example.learnservice.model.Semester;
import com.example.learnservice.repository.ClassroomAccountRepository;
import com.example.learnservice.repository.ClassroomDocumentRepository;
import com.example.learnservice.repository.ClassroomRepository;
import com.example.learnservice.repository.DocumentRepository;
import com.example.learnservice.repository.SemesterRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClassroomService {

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private ClassroomAccountRepository classroomAccountRepository;

    @Autowired
    private ClassroomDocumentRepository classroomDocumentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    public List<Classroom> getAllClassrooms() {
        return classroomRepository.findAll();
    }

    public List<Classroom> getClassroomsBySemester(Long semesterId) {
        return classroomRepository.findBySemesterId(semesterId);
    }

    public Classroom getClassroomById(Long classroomId) {
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        if (classroomOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom not found with id: " + classroomId);
        }
        return classroomOpt.get();
    }

    @Transactional
    public Classroom createClassroom(ClassroomCreateRequest createRequest, Long userId) {
        // Kiểm tra semester có tồn tại không
        Optional<Semester> semesterOpt = semesterRepository.findById(createRequest.getSemesterId());
        if (semesterOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Semester not found with id: " + createRequest.getSemesterId());
        }

        Semester semester = semesterOpt.get();

        Classroom classroom = new Classroom();
        classroom.setName(createRequest.getName());
        classroom.setSemester(semester);
        classroom.setCreatedBy(userId);
        classroom.setUpdatedBy(userId);
        if (classroom.getClassroomAccounts() == null) {
            classroom.setClassroomAccounts(new ArrayList<>());
        }
        ClassroomAccount classroomAccount = new ClassroomAccount();
        classroomAccount.setClassroom(classroom);
        classroomAccount.setAccountId(userId);
        classroom.getClassroomAccounts().add(classroomAccount);

        return classroomRepository.save(classroom);
    }

    @Transactional
    public void deleteClassroom(Long classroomId, Long userId) {
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        if (classroomOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom not found with id: " + classroomId);
        }

        Classroom classroom = classroomOpt.get();

        // Check if classroom has students or documents
        if (classroom.getClassroomAccounts() != null && !classroom.getClassroomAccounts().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete classroom with existing students. Please remove all students first.");
        }

        if (classroom.getClassroomDocuments() != null && !classroom.getClassroomDocuments().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete classroom with existing documents. Please remove all documents first.");
        }

        log.info("Deleting classroom: {} (ID: {}) by user: {}",
                classroom.getName(), classroom.getId(), userId);

        classroomRepository.delete(classroom);

        log.info("Classroom deleted successfully: {}", classroomId);
    }

    /**
     * Chỉ định account vào lớp học
     * Chỉ người tạo lớp mới có quyền chỉ định
     */
    @Transactional
    public void assignAccountsToClassroom(Long classroomId, ClassroomAccountRequest request, Long userId) {
        // Kiểm tra classroom tồn tại và quyền
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        if (classroomOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom not found with id: " + classroomId);
        }

        Classroom classroom = classroomOpt.get();

        // Chỉ người tạo lớp mới được chỉ định account
        if (!classroom.getCreatedBy().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the creator of the classroom can assign accounts");
        }

        // Lấy danh sách account hiện tại trong lớp
        Set<Long> currentAccountIds = classroom.getClassroomAccounts().stream()
                .map(ClassroomAccount::getAccountId)
                .collect(Collectors.toSet());

        Set<Long> requestedAccountIds = new HashSet<>(request.getAccountIds());

        // Tìm accounts cần thêm và cần xóa
        Set<Long> accountsToAdd = new HashSet<>(requestedAccountIds);
        accountsToAdd.removeAll(currentAccountIds);

        Set<Long> accountsToRemove = new HashSet<>(currentAccountIds);
        accountsToRemove.removeAll(requestedAccountIds);

        // Xóa accounts không còn trong danh sách
        if (!accountsToRemove.isEmpty()) {
            List<ClassroomAccount> accountsToDelete = classroom.getClassroomAccounts().stream()
                    .filter(ca -> accountsToRemove.contains(ca.getAccountId()))
                    .collect(Collectors.toList());

            classroomAccountRepository.deleteAll(accountsToDelete);
            classroom.getClassroomAccounts().removeAll(accountsToDelete);

            log.info("Removed {} accounts from classroom {}", accountsToRemove.size(), classroomId);
        }

        // Thêm accounts mới
        if (!accountsToAdd.isEmpty()) {
            List<ClassroomAccount> newClassroomAccounts = new ArrayList<>();
            for (Long accountId : accountsToAdd) {
                ClassroomAccount classroomAccount = new ClassroomAccount();
                classroomAccount.setAccountId(accountId);
                classroomAccount.setClassroom(classroom);
                newClassroomAccounts.add(classroomAccount);
            }

            List<ClassroomAccount> savedAccounts = classroomAccountRepository.saveAll(newClassroomAccounts);
            classroom.getClassroomAccounts().addAll(savedAccounts);

            log.info("Added {} accounts to classroom {}", accountsToAdd.size(), classroomId);
        }

        classroomRepository.save(classroom);
    }

    /**
     * Chỉ định tài liệu vào lớp học
     * Chỉ người tạo lớp mới có quyền chỉ định và tài liệu phải do người đó tải lên
     */
    @Transactional
    public void assignDocumentsToClassroom(Long classroomId, ClassroomDocumentRequest request, Long userId) {
        // Kiểm tra classroom tồn tại và quyền
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        if (classroomOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom not found with id: " + classroomId);
        }

        Classroom classroom = classroomOpt.get();

        // Chỉ người tạo lớp mới được chỉ định document
        if (!classroom.getCreatedBy().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the creator of the classroom can assign documents");
        }

        // Kiểm tra tất cả documents có tồn tại và thuộc về user không
        List<Document> documentsToAssign = new ArrayList<>();
        for (String documentCode : request.getDocumentCodes()) {
            Optional<Document> docOpt = documentRepository.findByCode(documentCode);
            if (docOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Document not found with code: " + documentCode);
            }

            Document document = docOpt.get();

            // Kiểm tra document có phải do user tải lên không
            if (!document.getCreatedBy().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Document with code " + documentCode + " was not uploaded by you");
            }

            documentsToAssign.add(document);
        }

        // Lấy danh sách document hiện tại trong lớp
        Set<String> currentDocumentCodes = classroom.getClassroomDocuments().stream()
                .map(cd -> cd.getDocument().getCode())
                .collect(Collectors.toSet());

        Set<String> requestedDocumentCodes = new HashSet<>(request.getDocumentCodes());

        // Tìm documents cần thêm và cần xóa
        Set<String> documentsToAdd = new HashSet<>(requestedDocumentCodes);
        documentsToAdd.removeAll(currentDocumentCodes);

        Set<String> documentsToRemove = new HashSet<>(currentDocumentCodes);
        documentsToRemove.removeAll(requestedDocumentCodes);

        // Xóa documents không còn trong danh sách
        if (!documentsToRemove.isEmpty()) {
            List<ClassroomDocument> documentsToDelete = classroom.getClassroomDocuments().stream()
                    .filter(cd -> documentsToRemove.contains(cd.getDocument().getCode()))
                    .collect(Collectors.toList());

            classroomDocumentRepository.deleteAll(documentsToDelete);
            classroom.getClassroomDocuments().removeAll(documentsToDelete);

            log.info("Removed {} documents from classroom {}", documentsToRemove.size(), classroomId);
        }

        // Thêm documents mới
        if (!documentsToAdd.isEmpty()) {
            List<ClassroomDocument> newClassroomDocuments = new ArrayList<>();
            for (Document document : documentsToAssign) {
                if (documentsToAdd.contains(document.getCode())) {
                    ClassroomDocument classroomDocument = new ClassroomDocument();
                    classroomDocument.setDocument(document);
                    classroomDocument.setClassroom(classroom);
                    newClassroomDocuments.add(classroomDocument);
                }
            }

            List<ClassroomDocument> savedDocuments = classroomDocumentRepository.saveAll(newClassroomDocuments);
            classroom.getClassroomDocuments().addAll(savedDocuments);

            log.info("Added {} documents to classroom {}", documentsToAdd.size(), classroomId);
        }

        classroomRepository.save(classroom);
    }

    /**
     * Lấy danh sách account trong lớp học
     */
    public List<Long> getAccountsInClassroom(Long classroomId) {
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        if (classroomOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom not found with id: " + classroomId);
        }

        return classroomOpt.get().getClassroomAccounts().stream()
                .map(ClassroomAccount::getAccountId)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách document trong lớp học
     */
    public List<String> getDocumentsInClassroom(Long classroomId) {
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        if (classroomOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom not found with id: " + classroomId);
        }

        return classroomOpt.get().getClassroomDocuments().stream()
                .map(cd -> cd.getDocument().getCode())
                .collect(Collectors.toList());
    }

    /*
     * Kiểm tra quyền truy cập classroom
     */
    public void checkClassroomAccess(long classroomId, Long accountId) {
        boolean exists = classroomAccountRepository.existsByClassroomIdAndAccountId(classroomId, accountId);
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not belong to this class");
        }
    }

}