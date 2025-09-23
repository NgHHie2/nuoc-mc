package com.example.learnservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.learnservice.dto.DocumentSearchDTO;
import com.example.learnservice.dto.SemesterCreateRequest;
import com.example.learnservice.dto.SemesterUpdateRequest;
import com.example.learnservice.enums.DocumentFormat;
import com.example.learnservice.dto.SemesterDetailDTO;
import com.example.learnservice.dto.SemesterSearchDTO;
import com.example.learnservice.model.Document;
import com.example.learnservice.model.Semester;
import com.example.learnservice.service.SemesterService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/semester")
public class SemesterController {

    @Autowired
    private SemesterService semesterService;

    /**
     * Tìm kiếm semester theo từ khóa, theo năm với phân trang
     * 
     * Input:
     * - keyword (optional): Từ khóa tìm kiếm trong name
     * - startYear (optional): Lọc theo năm bắt đầu
     * - endYear (optional): Lọc theo năm kết thúc
     * - searchFields (optional): Các trường cụ thể muốn tìm kiếm theo keyword
     * - pageable: Thông tin phân trang (page, size, sort)
     * 
     * Output:
     * - Page<SemesterDetailDTO>: Danh sách semester phân trang với metadata
     */
    @GetMapping("/search")
    public Page<SemesterDetailDTO> searchSemesters(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "startYear", required = false) Integer startYear,
            @RequestParam(value = "endYear", required = false) Integer endYear,
            @RequestParam(value = "searchFields", required = false) List<String> searchFields,
            Pageable pageable) {

        SemesterSearchDTO searchDTO = new SemesterSearchDTO();
        searchDTO.setKeyword(keyword);
        searchDTO.setStartYear(startYear);
        searchDTO.setEndYear(endYear);
        searchDTO.setSearchFields(searchFields);

        return semesterService.universalSearch(searchDTO, pageable);
    }

    @PostMapping("/")
    public ResponseEntity<?> createSemester(@Valid @RequestBody SemesterCreateRequest semesterCreateRequest,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long userId = Long.valueOf(userIdStr);
        Semester semester = semesterService.saveSemester(semesterCreateRequest, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", semester.getId());
        response.put("semesterName", semester.getName());
        response.put("startDate", semester.getStartDate());
        response.put("endDate", semester.getEndDate());
        response.put("createdAt", semester.getCreatedAt());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{semesterId}")
    public ResponseEntity<?> updateSemester(
            @PathVariable Long semesterId,
            @Valid @RequestBody SemesterUpdateRequest updateRequest,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long userId = Long.valueOf(userIdStr);
        Semester updatedSemester = semesterService.updateSemester(semesterId, updateRequest, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", updatedSemester.getId());
        response.put("semesterName", updatedSemester.getName());
        response.put("startDate", updatedSemester.getStartDate());
        response.put("endDate", updatedSemester.getEndDate());
        response.put("updatedAt", updatedSemester.getUpdatedAt());

        return ResponseEntity.ok(response);

    }

    @DeleteMapping("/{semesterId}")
    public ResponseEntity<?> deleteSemester(
            @PathVariable Long semesterId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long userId = Long.valueOf(userIdStr);
        semesterService.deleteSemester(semesterId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Semester deleted successfully");
        response.put("semesterId", semesterId);
        response.put("deletedAt", java.time.LocalDateTime.now());

        return ResponseEntity.ok(response);

    }
}