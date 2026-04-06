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

            String docId = node.get("documentId").asText();
            String text = node.get("text").asText();

            DocumentSnapshot snapshot = repository.findById(docId)
                    .orElse(new DocumentSnapshot());

            snapshot.setDocumentId(docId);
            snapshot.setContent(text);
            snapshot.setUpdatedAt(System.currentTimeMillis());

            repository.save(snapshot);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
