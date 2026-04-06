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

    @Column(columnDefinition = "TEXT")
    private String content;

    private long updatedAt;
}