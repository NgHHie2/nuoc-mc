package com.example.learnservice.dto;

import com.example.learnservice.model.Account;
import com.example.learnservice.model.Participation;
import com.example.learnservice.model.Subject;

import lombok.*;

@Data
@Builder
public class ParticipationDTO {
    private Integer id;
    private SubjectDTO subject;

    public static ParticipationDTO fromEntity(Participation participation) {
        if (participation == null)
            return null;

        return ParticipationDTO.builder()
                .id(participation.getId())
                .subject(SubjectDTO.fromEntity(participation.getSubject()))
                .build();
    }
}
