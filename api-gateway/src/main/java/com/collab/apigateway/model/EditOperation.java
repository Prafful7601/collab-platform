package com.collab.apigateway.model;

import lombok.Data;

@Data
public class EditOperation {

    private String type;        // insert, delete, replace, update, or title-update
    private int position;
    private String text;
    private String content;     // Legacy full-content updates
    private String title;
    private String clientId;
    private String operationId;
    private int length;
    private String documentId;
    private int version;
    private Long timestamp;
}
