package com.collab.apigateway.config;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.collab.apigateway.model.EditOperation;
import com.collab.apigateway.service.DocumentStateService;
import com.collab.apigateway.session.DocumentSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EditSocketHandler extends TextWebSocketHandler {

    private final DocumentStateService documentStateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EditSocketHandler(DocumentStateService documentStateService) {
        this.documentStateService = documentStateService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String docId = getDocumentId(session);
        DocumentSessionManager.joinDocument(docId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        // 1. Parse incoming edit
        EditOperation operation =
                objectMapper.readValue(message.getPayload(), EditOperation.class);

        String docId = operation.getDocumentId();

        // 2. Get current document from Redis
        String content = documentStateService.getDocument(docId);

        if (content == null) {
            content = "";
        }

        // 3. Apply operation
        if ("insert".equals(operation.getType())) {

            int pos = operation.getPosition();

            if (pos < 0) pos = 0;
            if (pos > content.length()) pos = content.length();

            content =
                    content.substring(0, pos)
                            + operation.getText()
                            + content.substring(pos);
        }

        // (delete coming next phase)

        // 4. Save updated document to Redis
        documentStateService.saveDocument(docId, content);

        // 5. Broadcast updated content to all users in same doc
        Set<WebSocketSession> sessions =
                DocumentSessionManager.getSessions(docId);

        String response = objectMapper.writeValueAsString(content);

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