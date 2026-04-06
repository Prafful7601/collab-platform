package com.collab.apigateway.model;

import lombok.Data;

@Data
public class EditOperation {

    private String type;        // insert or delete
    private int position;
    private String text;
    private int length;         // used for delete
    private String documentId;
    private int version;
}