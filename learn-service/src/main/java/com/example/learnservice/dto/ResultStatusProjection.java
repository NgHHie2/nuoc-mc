package com.example.learnservice.dto;

import java.time.LocalDateTime;

public interface ResultStatusProjection {
    Long getId();

    Long getStudentId();

    LocalDateTime getSubmitDateTime();

    Float getScore();
}
