package com.example.learnservice.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import com.example.learnservice.dto.SemesterCreateRequest;
import com.example.learnservice.dto.SemesterUpdateRequest;
import com.example.learnservice.dto.SemesterDetailDTO;
import com.example.learnservice.dto.SemesterSearchDTO;
import com.example.learnservice.model.ClassroomAccount;
import com.example.learnservice.model.Semester;
import com.example.learnservice.repository.SemesterRepository;
import com.example.learnservice.specification.SemesterSpecification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SemesterService {
    @Autowired
    private SemesterRepository semesterRepository;

    public List<Semester> getAllSemester() {
        return semesterRepository.findAll();
    }

    /**
     * Tìm kiếm semester với các tiêu chí đa dạng và phân trang
     * 
     * @param searchDTO - Chứa các tiêu chí tìm kiếm
     * @param pageable  - Thông tin phân trang
     * @return Page<SemesterDetailDTO> - Kết quả phân trang
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

            // Tính tổng số classroom
            Long totalClassrooms = semester.getClassrooms() != null ? (long) semester.getClassrooms().size() : 0L;
            dto.setTotalClassrooms(totalClassrooms);

            // Tính tổng số account duy nhất (từ tất cả classroom trong semester)
            Long totalAccounts = Optional.ofNullable(semester.getClassrooms())
                    .orElse(List.of()) // nếu null thì dùng list rỗng
                    .stream()
                    .flatMap(c -> Optional.ofNullable(c.getClassroomAccounts()).orElse(List.of()).stream())
                    .map(ClassroomAccount::getAccountId)
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

        // Validate dates
        if (updateRequest.getEndDate().isBefore(updateRequest.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate is before startDate");
        }

        // Update fields
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

        // Check if semester has classrooms
        if (semester.getClassrooms() != null && !semester.getClassrooms().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete semester with existing classrooms. Please delete all classrooms first.");
        }

        log.info("Deleting semester: {} (ID: {}) by user: {}",
                semester.getName(), semester.getId(), userId);

        semesterRepository.delete(semester);

        log.info("Semester deleted successfully: {}", semesterId);
    }
}