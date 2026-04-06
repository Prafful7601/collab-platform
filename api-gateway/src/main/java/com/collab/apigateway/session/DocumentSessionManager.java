package com.collab.apigateway.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.web.socket.WebSocketSession;

public class DocumentSessionManager {

    private static final Map<String, Set<WebSocketSession>> documentSessions = new HashMap<>();

    public static void joinDocument(String documentId, WebSocketSession session) {

        documentSessions
                .computeIfAbsent(documentId, k -> new HashSet<>())
                .add(session);
    }

    public static Set<WebSocketSession> getSessions(String documentId) {
        return documentSessions.getOrDefault(documentId, Collections.emptySet());
    }

    public static void removeSession(WebSocketSession session) {

        for (Set<WebSocketSession> sessions : documentSessions.values()) {
            sessions.remove(session);
        }
    }
}