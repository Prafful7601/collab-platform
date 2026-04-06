package com.collab.apigateway.config;

import java.util.List;
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
import com.collab.apigateway.session.DocumentSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EditSocketHandler extends TextWebSocketHandler {

    private final DocumentStateService documentStateService;
    private final EditEventProducer editEventProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EditSocketHandler(
            DocumentStateService documentStateService,
            EditEventProducer editEventProducer
    ) {
        this.documentStateService = documentStateService;
        this.editEventProducer = editEventProducer;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        String docId = getDocumentId(session);

        DocumentSessionManager.joinDocument(docId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        // 1. Parse operation from client
        EditOperation operation =
                objectMapper.readValue(message.getPayload(), EditOperation.class);

        String docId = operation.getDocumentId();

        // 2. Publish edit event to Kafka
        editEventProducer.publishEdit(
                objectMapper.writeValueAsString(operation)
        );

        // 3. Transform operation using history (Operational Transform)
        List<EditOperation> history =
                OperationHistory.getHistory(docId);

        for (EditOperation prev : history) {
            operation = OperationTransformer.transform(operation, prev);
        }

        // 4. Fetch current document state from Redis
        String content = documentStateService.getDocument(docId);

        if (content == null) {
            content = "";
        }

        // 5. APPLY INSERT
        if ("insert".equals(operation.getType())) {

            int pos = operation.getPosition();

            if (pos < 0) pos = 0;
            if (pos > content.length()) pos = content.length();

            content =
                    content.substring(0, pos)
                            + operation.getText()
                            + content.substring(pos);
        }

        // 6. APPLY DELETE
        if ("delete".equals(operation.getType())) {

            int pos = operation.getPosition();
            int len = operation.getLength();

            if (pos >= 0 && pos < content.length()) {

                int end = Math.min(pos + len, content.length());

                content =
                        content.substring(0, pos)
                                + content.substring(end);
            }
        }

        // 7. Save updated document to Redis
        documentStateService.saveDocument(docId, content);

        // 8. Save operation in history
        OperationHistory.addOperation(docId, operation);

        // 9. Broadcast updated document to all users editing the same document
        Set<WebSocketSession> sessions =
                DocumentSessionManager.getSessions(docId);

        String response =
                objectMapper.writeValueAsString(content);

        for (WebSocketSession s : sessions) {

            if (s.isOpen()) {
                s.sendMessage(new TextMessage(response));
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