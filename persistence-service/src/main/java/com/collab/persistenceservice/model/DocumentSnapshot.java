package com.collab.persistenceservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class DocumentSnapshot {

    @Id
    private String documentId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String editorMode;
    private String fileName;
    private String language;

    private int version;
    private long createdAt;
    private long updatedAt;
}
