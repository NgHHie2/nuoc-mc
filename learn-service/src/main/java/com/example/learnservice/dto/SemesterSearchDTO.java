package com.example.learnservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class SemesterSearchDTO {
    private String keyword; // Tìm kiếm trong tên semester
    private Integer startYear; // Lọc theo năm bắt đầu
    private Integer endYear; // Lọc theo năm kết thúc
    private List<String> searchFields; // Các trường cụ thể muốn tìm kiếm theo keyword
}