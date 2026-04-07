package com.collab.apigateway.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EditEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(EditEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public EditEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEdit(String message) {

        try {
            kafkaTemplate.send("document-edits", message);
        } catch (Exception exception) {
            logger.debug("Skipping Kafka publish because the broker is unavailable", exception);
        }
    }
}
