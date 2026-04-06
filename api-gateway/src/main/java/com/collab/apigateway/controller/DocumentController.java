package com.collab.apigateway.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collab.apigateway.model.Document;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private Map<String, Document> documents = new HashMap<>();

    @PostMapping
    public Document createDocument(@RequestBody Document doc) {

        String id = UUID.randomUUID().toString();
        doc.setId(id);

        documents.put(id, doc);

        return doc;
    }

    @GetMapping("/{id}")
    public Document getDocument(@PathVariable String id) {

        return documents.get(id);
    }
}