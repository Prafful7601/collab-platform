package com.collab.apigateway.session;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.WebSocketSession;

public class DocumentSessionManager {

    private static final Map<String, Set<WebSocketSession>> documentSessions = new ConcurrentHashMap<>();

    public static void joinDocument(String documentId, WebSocketSession session) {

        documentSessions
                .computeIfAbsent(documentId, key -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    public static Set<WebSocketSession> getSessions(String documentId) {
        return documentSessions.getOrDefault(documentId, Collections.emptySet());
    }

    public static int getParticipantCount(String documentId) {
        return getSessions(documentId).size();
    }

    public static void removeSession(WebSocketSession session) {

        documentSessions.entrySet().removeIf(entry -> {
            Set<WebSocketSession> sessions = entry.getValue();
            sessions.remove(session);
            return sessions.isEmpty();
        });
    }
}
