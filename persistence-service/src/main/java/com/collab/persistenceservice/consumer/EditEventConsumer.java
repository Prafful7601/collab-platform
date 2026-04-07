package com.collab.persistenceservice.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.collab.persistenceservice.model.DocumentSnapshot;
import com.collab.persistenceservice.repository.DocumentSnapshotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EditEventConsumer {

    private final DocumentSnapshotRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "document-edits", groupId = "persistence-service")
    public void consume(String message) {

        try {

            JsonNode node = objectMapper.readTree(message);
            JsonNode documentIdNode = node.get("documentId");

            if (documentIdNode == null || documentIdNode.isNull()) {
                return;
            }

            String docId = documentIdNode.asText();
            if (!node.hasNonNull("content")) {
                return;
            }

            String text = node.get("content").asText("");
            String title = node.hasNonNull("title") ? node.get("title").asText("Untitled document") : "Untitled document";
            int version = node.hasNonNull("version") ? node.get("version").asInt(0) : 0;
            long now = System.currentTimeMillis();

            DocumentSnapshot snapshot = repository.findById(docId)
                    .orElse(new DocumentSnapshot());

            snapshot.setDocumentId(docId);
            snapshot.setTitle(title);
            snapshot.setContent(text);
            snapshot.setVersion(version);
            snapshot.setCreatedAt(snapshot.getCreatedAt() > 0 ? snapshot.getCreatedAt() : now);
            snapshot.setUpdatedAt(now);

            repository.save(snapshot);

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
