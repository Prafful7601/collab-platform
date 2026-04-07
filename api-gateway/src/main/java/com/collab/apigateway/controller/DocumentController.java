package com.collab.apigateway.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collab.apigateway.model.Document;
import com.collab.apigateway.service.DocumentStateService;

@RestController
@RequestMapping("/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentStateService documentStateService;

    public DocumentController(DocumentStateService documentStateService) {
        this.documentStateService = documentStateService;
    }

    @GetMapping
    public List<Document> listDocuments() {
        return documentStateService.listDocuments();
    }

    @PostMapping
    public ResponseEntity<Document> createDocument(@RequestBody(required = false) Document doc) {

        Document document = doc != null ? doc : new Document();
        String id = StringUtils.hasText(document.getId()) ? document.getId() : UUID.randomUUID().toString();

        document.setId(id);

        return ResponseEntity.ok(documentStateService.saveDocument(document));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable String id) {

        Document document = documentStateService.getDocument(id);

        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(document);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(@PathVariable String id, @RequestBody Document document) {

        Document updatedDocument = documentStateService.updateDocument(id, document.getTitle(), document.getContent());

        return ResponseEntity.ok(updatedDocument);
    }
}
