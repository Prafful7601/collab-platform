package com.collab.apigateway.model;

import lombok.Data;

@Data
public class Document {

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
