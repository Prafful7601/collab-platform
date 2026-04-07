package com.collab.persistenceservice.controller;

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

import com.collab.persistenceservice.dto.StoredDocument;
import com.collab.persistenceservice.service.StoredDocumentService;

@RestController
@RequestMapping("/documents")
@CrossOrigin(origins = "*")
public class StoredDocumentController {

    private final StoredDocumentService storedDocumentService;

    public StoredDocumentController(StoredDocumentService storedDocumentService) {
        this.storedDocumentService = storedDocumentService;
    }

    @GetMapping
    public List<StoredDocument> listDocuments() {
        return storedDocumentService.listDocuments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoredDocument> getDocument(@PathVariable String id) {
        StoredDocument document = storedDocumentService.getDocument(id);
        return document != null ? ResponseEntity.ok(document) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<StoredDocument> createDocument(@RequestBody(required = false) StoredDocument request) {
        StoredDocument document = request != null ? request : new StoredDocument();
        String id = StringUtils.hasText(document.getId()) ? document.getId() : UUID.randomUUID().toString();
        document.setId(id);
        return ResponseEntity.ok(storedDocumentService.saveDocument(id, document));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoredDocument> updateDocument(@PathVariable String id, @RequestBody StoredDocument request) {
        return ResponseEntity.ok(storedDocumentService.saveDocument(id, request));
    }
}
