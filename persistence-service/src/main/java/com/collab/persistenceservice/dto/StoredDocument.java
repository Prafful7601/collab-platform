package com.collab.persistenceservice.dto;

import lombok.Data;

@Data
public class StoredDocument {

    private String id;
    private String title;
    private String content;
    private String editorMode;
    private String fileName;
    private String language;
    private int version;
    private long createdAt;
    private long updatedAt;
}
