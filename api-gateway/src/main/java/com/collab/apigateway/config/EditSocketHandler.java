package com.collab.apigateway.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.collab.apigateway.engine.OperationHistory;
import com.collab.apigateway.engine.OperationTransformer;
import com.collab.apigateway.event.EditEventProducer;
import com.collab.apigateway.model.Document;
import com.collab.apigateway.model.EditOperation;
import com.collab.apigateway.security.HtmlSanitizer;
import com.collab.apigateway.service.DocumentStateService;
import com.collab.apigateway.session.DocumentSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        session.getAttributes().put("docId", docId);

        DocumentSessionManager.joinDocument(docId, session);

        Document document = documentStateService.getOrCreateDocument(docId);

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                buildSnapshotPayload(document, "snapshot", null)
        )));

        broadcastPresence(docId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        EditOperation operation = objectMapper.readValue(message.getPayload(), EditOperation.class);

        String docId = StringUtils.hasText(operation.getDocumentId())
                ? operation.getDocumentId()
                : getDocumentId(session);
        operation.setDocumentId(docId);

        switch (operation.getType()) {
            case "insert":
            case "delete":
                handleTextOperation(operation, docId);
                return;
            case "title-update":
                handleTitleUpdate(operation, docId);
                return;
            case "update":
            case "replace":
                handleLegacySnapshot(operation, docId);
                return;
            default:
                return;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        String docId = (String) session.getAttributes().get("docId");
        DocumentSessionManager.removeSession(session);

        if (StringUtils.hasText(docId)) {
            try {
                broadcastPresence(docId);
            } catch (Exception ignored) {
                // Presence updates are best-effort during disconnect.
            }
        }
    }

    private void handleTextOperation(EditOperation incomingOperation, String docId) throws Exception {
        Document document = documentStateService.getOrCreateDocument(docId);
        EditOperation transformedOperation = normalizeOperation(incomingOperation, document.getVersion());

        List<EditOperation> operationsSinceBaseVersion = OperationHistory.getOperationsSince(docId, transformedOperation.getVersion());
        for (EditOperation historicalOperation : operationsSinceBaseVersion) {
            transformedOperation = OperationTransformer.transform(transformedOperation, historicalOperation);
        }

        String updatedContent = applyTextOperation(document.getContent(), transformedOperation);
        document.setContent(updatedContent);
        document.setVersion(document.getVersion() + 1);

        Document savedDocument = documentStateService.saveDocument(document);

        transformedOperation.setVersion(savedDocument.getVersion());
        transformedOperation.setTitle(savedDocument.getTitle());
        transformedOperation.setTimestamp(resolveTimestamp(incomingOperation.getTimestamp()));

        OperationHistory.addOperation(docId, transformedOperation);
        editEventProducer.publishEdit(objectMapper.writeValueAsString(buildOperationPayload(savedDocument, transformedOperation)));
        broadcastOperation(docId, savedDocument, transformedOperation);
    }

    private void handleTitleUpdate(EditOperation operation, String docId) throws Exception {
        Document updatedDocument = documentStateService.updateDocument(docId, sanitizeTitle(operation.getTitle()), null);

        operation.setVersion(updatedDocument.getVersion());
        operation.setTimestamp(resolveTimestamp(operation.getTimestamp()));
        operation.setTitle(updatedDocument.getTitle());

        editEventProducer.publishEdit(objectMapper.writeValueAsString(buildSnapshotPayload(updatedDocument, "title-update", operation.getClientId())));
        broadcastSnapshot(docId, updatedDocument, "title-update", operation.getClientId());
    }

    private void handleLegacySnapshot(EditOperation operation, String docId) throws Exception {
        String rawContent = operation.getContent() != null ? operation.getContent() : operation.getText();
        String sanitizedContent = htmlSanitizer.sanitize(rawContent != null ? rawContent : "");
        Document updatedDocument = documentStateService.updateDocument(docId, sanitizeTitle(operation.getTitle()), sanitizedContent);

        operation.setVersion(updatedDocument.getVersion());
        operation.setTimestamp(resolveTimestamp(operation.getTimestamp()));
        operation.setContent(updatedDocument.getContent());
        operation.setTitle(updatedDocument.getTitle());

        editEventProducer.publishEdit(objectMapper.writeValueAsString(buildSnapshotPayload(updatedDocument, "snapshot", operation.getClientId())));
        broadcastSnapshot(docId, updatedDocument, "snapshot", operation.getClientId());
    }

    private EditOperation normalizeOperation(EditOperation incomingOperation, int currentVersion) {
        EditOperation normalizedOperation = new EditOperation();
        normalizedOperation.setType(incomingOperation.getType());
        normalizedOperation.setPosition(Math.max(0, incomingOperation.getPosition()));
        normalizedOperation.setLength(Math.max(0, incomingOperation.getLength()));
        normalizedOperation.setText(incomingOperation.getText() != null ? incomingOperation.getText() : "");
        normalizedOperation.setClientId(incomingOperation.getClientId());
        normalizedOperation.setOperationId(incomingOperation.getOperationId());
        normalizedOperation.setDocumentId(incomingOperation.getDocumentId());
        normalizedOperation.setVersion(Math.max(0, incomingOperation.getVersion()));
        normalizedOperation.setTimestamp(resolveTimestamp(incomingOperation.getTimestamp()));

        if (normalizedOperation.getVersion() > currentVersion) {
            normalizedOperation.setVersion(currentVersion);
        }

        return normalizedOperation;
    }

    private String applyTextOperation(String content, EditOperation operation) {
        String safeContent = content != null ? content : "";
        int position = Math.min(Math.max(0, operation.getPosition()), safeContent.length());

        if ("insert".equals(operation.getType())) {
            return safeContent.substring(0, position) + operation.getText() + safeContent.substring(position);
        }

        if ("delete".equals(operation.getType())) {
            int end = Math.min(position + operation.getLength(), safeContent.length());
            return safeContent.substring(0, position) + safeContent.substring(end);
        }

        return safeContent;
    }

    private String getDocumentId(WebSocketSession session) {

        String query = session.getUri() != null ? session.getUri().getQuery() : null;

        if (query == null) {
            return "default";
        }

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && "docId".equals(parts[0]) && StringUtils.hasText(parts[1])) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }

        return "default";
    }

    private void broadcastOperation(String docId, Document document, EditOperation operation) throws Exception {
        Set<WebSocketSession> sessions = DocumentSessionManager.getSessions(docId);
        String payload = objectMapper.writeValueAsString(buildOperationPayload(document, operation));

        for (WebSocketSession currentSession : sessions) {
            if (currentSession.isOpen()) {
                currentSession.sendMessage(new TextMessage(payload));
            }
        }
    }

    private void broadcastSnapshot(String docId, Document document, String type, String clientId) throws Exception {
        Set<WebSocketSession> sessions = DocumentSessionManager.getSessions(docId);
        String payload = objectMapper.writeValueAsString(buildSnapshotPayload(document, type, clientId));

        for (WebSocketSession currentSession : sessions) {
            if (currentSession.isOpen()) {
                currentSession.sendMessage(new TextMessage(payload));
            }
        }
    }

    private void broadcastPresence(String docId) throws Exception {
        Set<WebSocketSession> sessions = DocumentSessionManager.getSessions(docId);
        String payload = objectMapper.writeValueAsString(Map.of(
                "type", "presence",
                "documentId", docId,
                "participants", DocumentSessionManager.getParticipantCount(docId)
        ));

        for (WebSocketSession currentSession : sessions) {
            if (currentSession.isOpen()) {
                currentSession.sendMessage(new TextMessage(payload));
            }
        }
    }

    private Map<String, Object> buildSnapshotPayload(Document document, String type, String clientId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("documentId", document.getId());
        payload.put("title", document.getTitle());
        payload.put("content", document.getContent());
        payload.put("version", document.getVersion());
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("participants", DocumentSessionManager.getParticipantCount(document.getId()));

        if (clientId != null) {
            payload.put("clientId", clientId);
        }

        return payload;
    }

    private Map<String, Object> buildOperationPayload(Document document, EditOperation operation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "operation");
        payload.put("opType", operation.getType());
        payload.put("documentId", document.getId());
        payload.put("position", operation.getPosition());
        payload.put("length", operation.getLength());
        payload.put("text", operation.getText());
        payload.put("title", document.getTitle());
        payload.put("version", operation.getVersion());
        payload.put("timestamp", operation.getTimestamp());
        payload.put("participants", DocumentSessionManager.getParticipantCount(document.getId()));
        payload.put("clientId", operation.getClientId());
        payload.put("operationId", operation.getOperationId());
        return payload;
    }

    private String sanitizeTitle(String title) {
        String normalized = title != null ? title.replaceAll("[\\r\\n]+", " ").trim() : "";
        return StringUtils.hasText(normalized) ? normalized : "Untitled document";
    }

    private long resolveTimestamp(Long timestamp) {
        return timestamp != null ? timestamp : System.currentTimeMillis();
    }
}
