package com.collab.apigateway.config;

import java.util.Set;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.collab.apigateway.model.EditOperation;
import com.collab.apigateway.session.DocumentSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EditSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        EditOperation operation =
                objectMapper.readValue(message.getPayload(), EditOperation.class);

        String documentId = operation.getDocumentId();

        Set<WebSocketSession> sessions =
                DocumentSessionManager.getSessions(documentId);

        String json = objectMapper.writeValueAsString(operation);

        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(json));
            }
        }
    }
}