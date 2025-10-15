// ================ UPDATED SEMESTER SERVICE ================

package com.example.learnservice.service;

import java.util.ArrayList;
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

import com.example.learnservice.client.AccountClient;
import com.example.learnservice.dto.AccountDTO;
import com.example.learnservice.dto.SemesterAccountRequest;
import com.example.learnservice.dto.SemesterCreateRequest;
import com.example.learnservice.dto.SemesterUpdateRequest;
import com.example.learnservice.enums.Role;
import com.example.learnservice.dto.SemesterDetailDTO;
import com.example.learnservice.dto.SemesterDocumentRequest;
import com.example.learnservice.dto.SemesterSearchDTO;
import com.example.learnservice.dto.SemesterTeacherRequest;
import com.example.learnservice.model.Document;
import com.example.learnservice.model.Position;
import com.example.learnservice.model.Semester;
import com.example.learnservice.model.SemesterAccount;
import com.example.learnservice.model.SemesterDocument;
import com.example.learnservice.model.SemesterTeacher;
import com.example.learnservice.repository.DocumentRepository;
import com.example.learnservice.repository.PositionRepository;
import com.example.learnservice.repository.SemesterAccountRepository;
import com.example.learnservice.repository.SemesterDocumentRepository;
import com.example.learnservice.repository.SemesterRepository;
import com.example.learnservice.repository.SemesterTeacherRepository;
import com.example.learnservice.specification.SemesterSpecification;
import com.example.learnservice.util.ValidateUtil;

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

    @Autowired
    private SemesterTeacherRepository semesterTeacherRepository;

    @Autowired
    private AccountClient accountClient;

    public List<Semester> getAllSemester() {
        return semesterRepository.findAll();
    }

    /**
     * Tìm kiếm semester với các tiêu chí đa dạng và phân trang
     */
    public Page<SemesterDetailDTO> universalSearch(SemesterSearchDTO searchDTO, Pageable pageable, Long userId,
            Role userRole) {
        searchDTO.setKeyword(ValidateUtil.validateKeyword(searchDTO.getKeyword()));
        searchDTO.setSearchFields(ValidateUtil.validateSearchFields(searchDTO.getSearchFields()));
        Sort sort = pageable.getSort().and(Sort.by(Sort.Direction.DESC, "createdAt"));
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Specification<Semester> spec = SemesterSpecification.build(searchDTO);

        // Apply role-based filtering
        if (userRole == Role.STUDENT) {
            // Student chỉ thấy semester mà họ được assign vào
            spec = spec.and(SemesterSpecification.hasStudent(userId));
        } else if (userRole == Role.TEACHER) {
            // Teacher chỉ thấy semester mà họ được assign vào
            spec = spec.and(SemesterSpecification.hasTeacher(userId));
        }
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

    @Transactional
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

        // List<SemesterTeacher> semesterTeachers = validateAndPrepareTeachers(
        // semester,
        // semesterCreateRequest.getTeacherIds(),
        // userId);

        Semester savedSemester = semesterRepository.save(semester);

        // if (!semesterTeachers.isEmpty()) {
        // semesterTeachers.forEach(st -> st.setSemester(savedSemester));
        // if (savedSemester.getSemesterTeachers() == null) {
        // savedSemester.setSemesterTeachers(new ArrayList<>());
        // }
        // savedSemester.getSemesterTeachers().addAll(semesterTeachers);
        // log.info("Assigned {} teachers to semester {}", semesterTeachers.size(),
        // savedSemester.getId());
        // }

        return savedSemester;
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

        // Update teachers nếu có trong request
        // if (updateRequest.getTeacherIds() != null) {
        // List<SemesterTeacher> newTeachers = validateAndPrepareTeachers(
        // semester,
        // updateRequest.getTeacherIds(),
        // userId);

        // if (semester.getSemesterTeachers() != null) {
        // semester.getSemesterTeachers().clear();
        // } else {
        // semester.setSemesterTeachers(new ArrayList<>());
        // }

        // if (!newTeachers.isEmpty()) {
        // newTeachers.forEach(st -> st.setSemester(semester));
        // semester.getSemesterTeachers().addAll(newTeachers);
        // log.info("Updated {} teachers for semester {}", newTeachers.size(),
        // semesterId);
        // }
        // }

        return semesterRepository.save(semester);
    }

    /**
     * Xóa teacher khỏi semester
     */
    @Transactional
    public void removeTeacherFromSemester(Long semesterId, Long teacherId, Long userId) {
        int deletedCount = semesterTeacherRepository.deleteBySemesterIdAndTeacherId(semesterId, teacherId);

        if (deletedCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found in semester");
        }

        log.info("Removed teacher {} from semester {} by user {}", teacherId, semesterId, userId);
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

        // List<SemesterDocument> saved =
        // semesterDocumentRepository.saveAll(newSemesterDocuments);
        semester.getSemesterDocuments().addAll(newSemesterDocuments);

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

        // semesterDocumentRepository.delete(toRemove);
        semester.getSemesterDocuments().remove(toRemove);

        log.info("Removed document {} from semester {} by user {}", documentCode, semesterId, userId);
    }

    /**
     * Thêm accounts vào semester (không xóa cũ)
     */
    @Transactional
    public void addAccountsToSemester(Long semesterId, SemesterAccountRequest request, Long userId) {
        Semester semester = getSemesterById(semesterId);

        // Lấy danh sách accountIds từ request
        List<Long> accountIds = request.getAccountAssignments().stream()
                .map(SemesterAccountRequest.AccountPositionAssignment::getAccountId)
                .distinct()
                .collect(Collectors.toList());

        // Validate tất cả accounts phải có role STUDENT
        List<AccountDTO> validAccs = validateStudentAccounts(accountIds);
        if (validAccs.isEmpty() || validAccs.size() == 0)
            throw new IllegalArgumentException("Account is not student");

        // Get current account-position pairs
        Set<String> currentPairs = semester.getSemesterAccounts().stream()
                .map(sa -> sa.getAccountId().toString())
                .collect(Collectors.toSet());

        // Filter new assignments
        List<SemesterAccountRequest.AccountPositionAssignment> newAssignments = request.getAccountAssignments().stream()
                .filter(assignment -> !currentPairs.contains(
                        assignment.getAccountId().toString()))
                .collect(Collectors.toList());

        if (newAssignments.isEmpty())
            throw new IllegalArgumentException("No new students");

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
        log.info("Added {} students to semester {}", newAssignments.size(), semesterId);
    }

    @Transactional
    public void addTeachersToSemester(Long semesterId, SemesterTeacherRequest request, Long userId) {
        Semester semester = getSemesterById(semesterId);

        // Lấy danh sách accountIds từ request
        List<Long> accountIds = request.getTeacherIds().stream()
                .distinct()
                .collect(Collectors.toList());

        // Validate tất cả accounts phải có role STUDENT
        List<AccountDTO> validAccs = validateTeacherAccounts(accountIds);
        if (validAccs.isEmpty() || validAccs.size() == 0)
            throw new IllegalArgumentException("Account is not teacher");

        List<Long> currentTeacherIds = semester.getSemesterTeachers().stream().map(SemesterTeacher::getTeacherId)
                .toList();
        List<Long> newTeacherIds = validAccs.stream().map(AccountDTO::getId)
                .filter(id -> !currentTeacherIds.contains(id)).toList();

        if (newTeacherIds.isEmpty())
            throw new IllegalArgumentException("No new teachers");

        // Create new assignments
        List<SemesterTeacher> newSemesterTeachers = validAccs.stream()
                .map(acc -> {
                    SemesterTeacher st = new SemesterTeacher();
                    st.setSemester(semester);
                    st.setTeacherId(acc.getId());
                    st.setCreatedBy(userId);
                    return st;
                }).collect(Collectors.toList());

        semesterTeacherRepository.saveAll(newSemesterTeachers);
        log.info("Added {} teachers to semester {}", newSemesterTeachers.size(), semesterId);
    }

    /**
     * Xóa account khỏi semester
     */
    @Transactional
    public void removeAccountFromSemester(Long semesterId, Long accountId, Long userId) {
        int deletedCount = semesterAccountRepository.deleteBySemesterIdAndAccountId(semesterId, accountId);

        if (deletedCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found in semester");
        }

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

    /**
     * Kiểm tra teacher có quyền truy cập semester không
     */
    public boolean checkSemesterAccessWithTeacher(Long semesterId, Long accountId) {
        return semesterTeacherRepository.existsBySemesterIdAndTeacherId(semesterId, accountId);
    }

    /**
     * Kiểm tra student có quyền truy cập semester không
     */
    public Optional<SemesterAccount> checkSemesterAccessWithStudent(Long semesterId, Long accountId) {
        return semesterAccountRepository.findBySemesterIdAndAccountId(semesterId, accountId);
    }

    /**
     * // * Validate teachers trước khi assign
     * // * Trả về list SemesterTeacher entities đã sẵn sàng để lưu
     * //
     */
    // private List<SemesterTeacher> validateAndPrepareTeachers(Semester semester,
    // List<Long> teacherIds, Long userId) {
    // if (teacherIds == null || teacherIds.isEmpty()) {
    // return List.of();
    // }

    // // Gọi AccountClient để lấy thông tin accounts
    // List<AccountDTO> accounts = accountClient.getAccountsByIds(teacherIds);

    // // Lọc ra các account có role TEACHER
    // List<AccountDTO> validTeachers = accounts.stream()
    // .filter(acc -> acc.getRole() == Role.TEACHER)
    // .collect(Collectors.toList());

    // // Kiểm tra xem tất cả IDs có đúng là TEACHER không
    // if (validTeachers.size() != teacherIds.size()) {
    // List<Long> validTeacherIds = validTeachers.stream()
    // .map(AccountDTO::getId)
    // .collect(Collectors.toList());

    // List<Long> invalidIds = teacherIds.stream()
    // .filter(id -> !validTeacherIds.contains(id))
    // .collect(Collectors.toList());

    // throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
    // "Invalid teacher IDs (not TEACHER role or not found): " + invalidIds);
    // }

    // // Tạo SemesterTeacher entities (chưa lưu DB)
    // return validTeachers.stream()
    // .map(teacher -> {
    // SemesterTeacher st = new SemesterTeacher();
    // st.setSemester(semester);
    // st.setTeacherId(teacher.getId());
    // st.setCreatedBy(userId);
    // return st;
    // })
    // .collect(Collectors.toList());
    // }

    /**
     * Validate accounts trước khi assign vào semester
     * Chỉ chấp nhận accounts có role STUDENT
     */
    private List<AccountDTO> validateStudentAccounts(List<Long> accountIds) {
        // Gọi AccountClient để lấy thông tin accounts
        List<AccountDTO> accounts = accountClient.getAccountsByIds(accountIds);

        // Lọc ra các account có role STUDENT
        List<AccountDTO> validStudents = accounts.stream()
                .filter(acc -> acc.getRole() == Role.STUDENT)
                .collect(Collectors.toList());

        return validStudents;
    }

    /**
     * Validate accounts trước khi assign vào semester
     * Chỉ chấp nhận accounts có role TEACHER
     */
    private List<AccountDTO> validateTeacherAccounts(List<Long> accountIds) {
        // Gọi AccountClient để lấy thông tin accounts
        List<AccountDTO> accounts = accountClient.getAccountsByIds(accountIds);

        // Lọc ra các account có role TEACHER
        List<AccountDTO> validTeachers = accounts.stream()
                .filter(acc -> acc.getRole() == Role.TEACHER)
                .collect(Collectors.toList());

        return validTeachers;
    }

}