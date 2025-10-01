package com.example.learnservice.util.listener.event;

import com.example.learnservice.dto.DocumentDTO;
import com.example.learnservice.model.Document;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class DocumentDeletedEvent {
    private DocumentDTO document;

    public DocumentDeletedEvent(Document doc) {
        this.document.setId(doc.getId());
        this.document.setFormat(doc.getFormat());
    }
}
