package com.example.learnservice.dto;

import com.example.learnservice.model.Participation;
import com.example.learnservice.model.Subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class SubjectDTO {
    private Integer id;

    private String title;
    private String code;
    private String description;

    public static SubjectDTO fromEntity(Subject subject) {
        if (subject == null)
            return null;

        return SubjectDTO.builder()
                .id(subject.getId())
                .title(subject.getTitle())
                .code(subject.getCode())
                .description(subject.getDescription())
                .build();
    }
}
