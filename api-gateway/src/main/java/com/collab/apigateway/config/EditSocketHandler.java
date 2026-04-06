package com.collab.apigateway.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.collab.apigateway.engine.OperationHistory;
import com.collab.apigateway.engine.OperationTransformer;
import com.collab.apigateway.event.EditEventProducer;
import com.collab.apigateway.model.EditOperation;
import com.collab.apigateway.service.DocumentStateService;
import com.collab.apigateway.security.HtmlSanitizer;

@Component
public class EditSocketHandler extends TextWebSocketHandler {

    private final DocumentStateService documentStateService;
    private final EditEventProducer editEventProducer;
    private final HtmlSanitizer htmlSanitizer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EditSocketHandler(
            DocumentStateService documentStateService,
            EditEventProducer editEventProducer,
            HtmlSanitizer htmlSanitizer
    ) {
        this.documentStateService = documentStateService;
        this.editEventProducer = editEventProducer;
        this.htmlSanitizer = htmlSanitizer;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        String docId = getDocumentId(session);

        // Join document session
        DocumentSessionManager.joinDocument(docId, session);

        // Fetch current document from Redis
        String content = documentStateService.getDocument(docId);

        if (content == null) {
            content = "";
        }

        // Send latest document to the newly connected user as JSON
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
            Map.of(
                "type", "update",
                "content", content,
                "timestamp", System.currentTimeMillis()
            )
        )));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        // Parse operation
        EditOperation operation =
                objectMapper.readValue(message.getPayload(), EditOperation.class);

        String docId = operation.getDocumentId();

        // For HTML content, we use full document replacement
        if ("update".equals(operation.getType())) {
            String htmlContent = operation.getContent() != null ? operation.getContent() : operation.getText();

            // Sanitize HTML content to prevent XSS attacks
            htmlContent = htmlSanitizer.sanitize(htmlContent);

            // Save to Redis
            documentStateService.saveDocument(docId, htmlContent);

            // Publish to Kafka for persistence
            editEventProducer.publishEdit(
                objectMapper.writeValueAsString(operation)
            );

            // Broadcast updated HTML content to all connected clients
            Set<WebSocketSession> sessions = DocumentSessionManager.getSessions(docId);

            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        Map.of(
                            "type", "update",
                            "content", htmlContent,
                            "timestamp", operation.getTimestamp() != null ? operation.getTimestamp() : System.currentTimeMillis()
                        )
                    )));
                }
            }
        }
        // Legacy support for old operation types (insert/delete/replace)
        else {
            // Publish to Kafka
            editEventProducer.publishEdit(
                    objectMapper.writeValueAsString(operation)
            );

            // Transform using OT
            List<EditOperation> history = OperationHistory.getHistory(docId);

            for (EditOperation prev : history) {
                operation = OperationTransformer.transform(operation, prev);
            }

            // Get document from Redis
            String content = documentStateService.getDocument(docId);

            if (content == null) {
                content = "";
            }

            // REPLACE - full document replacement
            if ("replace".equals(operation.getType())) {
                content = operation.getText();
            }

            // INSERT
            if ("insert".equals(operation.getType())) {
                int pos = operation.getPosition();
                if (pos < 0) pos = 0;
                if (pos > content.length()) pos = content.length();
                content = content.substring(0, pos) + operation.getText() + content.substring(pos);
            }

            // DELETE
            if ("delete".equals(operation.getType())) {
                int pos = operation.getPosition();
                int len = operation.getLength();
                if (pos >= 0 && pos < content.length()) {
                    int end = Math.min(pos + len, content.length());
                    content = content.substring(0, pos) + content.substring(end);
                }
            }

            // Save to Redis
            documentStateService.saveDocument(docId, content);

            // Save history
            OperationHistory.addOperation(docId, operation);

            // Broadcast updated document
            Set<WebSocketSession> sessions = DocumentSessionManager.getSessions(docId);

            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(content));
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        DocumentSessionManager.removeSession(session);
    }

    private String getDocumentId(WebSocketSession session) {

        String query = session.getUri().getQuery();

        if (query == null) {
            return "default";
        }

        return query.split("=")[1];
    }
}