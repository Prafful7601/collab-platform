package com.collab.apigateway.model;

import lombok.Data;

@Data
public class EditOperation {

    private String type;      // insert / delete
    private int position;
    private String text;
    private String documentId;
}