package com.collab.apigateway.model;

import lombok.Data;

@Data
public class EditOperation {

    private String type;        // insert, delete, replace, or update
    private int position;
    private String text;
    private String content;     // For HTML content updates
    private int length;         // used for delete
    private String documentId;
    private int version;
    private Long timestamp;     // For tracking update timestamps
}