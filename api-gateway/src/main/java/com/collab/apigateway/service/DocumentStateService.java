package com.collab.apigateway.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import com.collab.apigateway.model.Document;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DocumentStateService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DocumentPersistenceClient documentPersistenceClient;
    private final Map<String, Document> localDocuments = new ConcurrentHashMap<>();

    public DocumentStateService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            DocumentPersistenceClient documentPersistenceClient
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.documentPersistenceClient = documentPersistenceClient;
    }

    public List<Document> listDocuments() {
        try {
            List<Document> documents = documentPersistenceClient.listDocuments().stream()
                    .map(this::normalizeDocument)
                    .toList();

            documents.forEach(document -> localDocuments.put(document.getId(), copyDocument(document)));
            return documents;
        } catch (RestClientException ignored) {
            return localDocuments.values().stream()
                    .map(this::copyDocument)
                    .sorted(Comparator.comparingLong(Document::getUpdatedAt).reversed())
                    .toList();
        }
    }

    public Document getDocument(String docId) {
        Document cachedDocument = localDocuments.get(docId);

        if (cachedDocument != null) {
            return copyDocument(cachedDocument);
        }

        try {
            Document persistedDocument = normalizeDocument(documentPersistenceClient.getDocument(docId));
            localDocuments.put(docId, copyDocument(persistedDocument));
            saveRedisSnapshot(persistedDocument);
            return persistedDocument;
        } catch (RestClientException ignored) {
            // Fall back to Redis/local cache so the editor keeps working.
        }

        try {
            String payload = redisTemplate.opsForValue().get(redisKey(docId));

            if (!StringUtils.hasText(payload)) {
                return null;
            }

            Document redisDocument = objectMapper.readValue(payload, Document.class);
            Document normalizedDocument = normalizeDocument(redisDocument);
            localDocuments.put(docId, copyDocument(normalizedDocument));
            return normalizedDocument;
        } catch (DataAccessException | JsonProcessingException ignored) {
            return cachedDocument != null ? copyDocument(cachedDocument) : null;
        }
    }

    public Document getOrCreateDocument(String docId) {
        Document existingDocument = getDocument(docId);

        if (existingDocument != null) {
            return existingDocument;
        }

        long now = System.currentTimeMillis();
        Document document = new Document();
        document.setId(docId);
        document.setTitle("Untitled document");
        document.setContent("");
        document.setEditorMode("doc");
        document.setFileName("notes.md");
        document.setLanguage("markdown");
        document.setVersion(0);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        return saveDocument(document);
    }

    public Document saveDocument(Document document) {
        Document normalizedDocument = normalizeDocument(document);
        localDocuments.put(normalizedDocument.getId(), copyDocument(normalizedDocument));
        saveRedisSnapshot(normalizedDocument);

        try {
            Document persistedDocument = normalizeDocument(documentPersistenceClient.saveDocument(normalizedDocument));
            localDocuments.put(persistedDocument.getId(), copyDocument(persistedDocument));
            saveRedisSnapshot(persistedDocument);
            return copyDocument(persistedDocument);
        } catch (RestClientException ignored) {
            return copyDocument(normalizedDocument);
        }
    }

    public Document updateDocument(String docId, String title, String content, String editorMode, String fileName, String language) {
        Document document = getOrCreateDocument(docId);

        if (title != null) {
            document.setTitle(title);
        }

        if (content != null) {
            document.setContent(content);
        }

        if (editorMode != null) {
            document.setEditorMode(editorMode);
        }

        if (fileName != null) {
            document.setFileName(fileName);
        }

        if (language != null) {
            document.setLanguage(language);
        }

        document.setVersion(document.getVersion() + 1);
        document.setUpdatedAt(System.currentTimeMillis());
        return saveDocument(document);
    }

    private void saveRedisSnapshot(Document document) {
        try {
            redisTemplate.opsForValue().set(redisKey(document.getId()), objectMapper.writeValueAsString(document));
        } catch (DataAccessException | JsonProcessingException ignored) {
            // Redis is an optimization, not a hard dependency.
        }
    }

    private String redisKey(String docId) {
        return "doc:" + docId;
    }

    private Document normalizeDocument(Document source) {
        long now = System.currentTimeMillis();
        Document document = new Document();
        document.setId(source.getId());
        document.setTitle(StringUtils.hasText(source.getTitle()) ? source.getTitle().trim() : "Untitled document");
        document.setContent(source.getContent() != null ? source.getContent() : "");
        document.setEditorMode(normalizeEditorMode(source.getEditorMode()));
        document.setFileName(normalizeFileName(source.getFileName(), document.getEditorMode()));
        document.setLanguage(normalizeLanguage(source.getLanguage(), document.getEditorMode()));
        document.setVersion(Math.max(0, source.getVersion()));
        document.setCreatedAt(source.getCreatedAt() > 0 ? source.getCreatedAt() : now);
        document.setUpdatedAt(source.getUpdatedAt() > 0 ? source.getUpdatedAt() : document.getCreatedAt());
        return document;
    }

    private Document copyDocument(Document source) {
        Document document = new Document();
        document.setId(source.getId());
        document.setTitle(source.getTitle());
        document.setContent(source.getContent());
        document.setEditorMode(source.getEditorMode());
        document.setFileName(source.getFileName());
        document.setLanguage(source.getLanguage());
        document.setVersion(source.getVersion());
        document.setCreatedAt(source.getCreatedAt());
        document.setUpdatedAt(source.getUpdatedAt());
        return document;
    }

    private String normalizeEditorMode(String editorMode) {
        return "code".equalsIgnoreCase(editorMode) ? "code" : "doc";
    }

    private String normalizeFileName(String fileName, String editorMode) {
        if (StringUtils.hasText(fileName)) {
            return fileName.trim();
        }

        return "code".equals(editorMode) ? "main.js" : "notes.md";
    }

    private String normalizeLanguage(String language, String editorMode) {
        if (StringUtils.hasText(language)) {
            return language.trim().toLowerCase();
        }

        return "code".equals(editorMode) ? "javascript" : "markdown";
    }
}
