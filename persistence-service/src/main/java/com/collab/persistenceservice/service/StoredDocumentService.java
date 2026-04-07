package com.collab.persistenceservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.collab.persistenceservice.dto.StoredDocument;
import com.collab.persistenceservice.model.DocumentSnapshot;
import com.collab.persistenceservice.repository.DocumentSnapshotRepository;

@Service
public class StoredDocumentService {

    private final DocumentSnapshotRepository repository;

    public StoredDocumentService(DocumentSnapshotRepository repository) {
        this.repository = repository;
    }

    public List<StoredDocument> listDocuments() {
        return repository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    public StoredDocument getDocument(String documentId) {
        return repository.findById(documentId)
                .map(this::toDto)
                .orElse(null);
    }

    public StoredDocument saveDocument(String documentId, StoredDocument request) {
        long now = System.currentTimeMillis();

        DocumentSnapshot snapshot = repository.findById(documentId)
                .orElse(new DocumentSnapshot());

        snapshot.setDocumentId(documentId);
        snapshot.setTitle(normalizeTitle(request.getTitle()));
        snapshot.setContent(request.getContent() != null ? request.getContent() : "");
        snapshot.setVersion(Math.max(0, request.getVersion()));
        snapshot.setCreatedAt(snapshot.getCreatedAt() > 0 ? snapshot.getCreatedAt() : now);
        snapshot.setUpdatedAt(now);

        return toDto(repository.save(snapshot));
    }

    private StoredDocument toDto(DocumentSnapshot snapshot) {
        StoredDocument document = new StoredDocument();
        document.setId(snapshot.getDocumentId());
        document.setTitle(snapshot.getTitle());
        document.setContent(snapshot.getContent());
        document.setVersion(snapshot.getVersion());
        document.setCreatedAt(snapshot.getCreatedAt());
        document.setUpdatedAt(snapshot.getUpdatedAt());
        return document;
    }

    private String normalizeTitle(String title) {
        return StringUtils.hasText(title) ? title.trim() : "Untitled document";
    }
}
