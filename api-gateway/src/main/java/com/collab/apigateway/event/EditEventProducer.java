package com.collab.apigateway.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EditEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public EditEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEdit(String message) {

        kafkaTemplate.send("document-edits", message);
    }
}