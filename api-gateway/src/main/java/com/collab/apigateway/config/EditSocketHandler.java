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

        // 1. Parse incoming operation
        EditOperation operation =
                objectMapper.readValue(message.getPayload(), EditOperation.class);

        String docId = operation.getDocumentId();

        // 2. Transform operation using history (Operational Transform)
        List<EditOperation> history =
                OperationHistory.getHistory(docId);

        for (EditOperation prev : history) {
            operation = OperationTransformer.transform(operation, prev);
        }

        // 3. Fetch document from Redis
        String content = documentStateService.getDocument(docId);

        if (content == null) {
            content = "";
        }

        // 4. APPLY INSERT
        if ("insert".equals(operation.getType())) {

            int pos = operation.getPosition();

            if (pos < 0) pos = 0;
            if (pos > content.length()) pos = content.length();

            content =
                    content.substring(0, pos)
                            + operation.getText()
                            + content.substring(pos);
        }

        // 5. APPLY DELETE
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

        // 6. Save updated document in Redis
        documentStateService.saveDocument(docId, content);

        // 7. Save operation in history
        OperationHistory.addOperation(docId, operation);

        // 8. Broadcast updated content to document session
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