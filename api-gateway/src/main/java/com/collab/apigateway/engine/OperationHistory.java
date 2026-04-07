package com.collab.apigateway.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.collab.apigateway.model.EditOperation;

public class OperationHistory {

    private static final Map<String, List<EditOperation>> history = new ConcurrentHashMap<>();

    public static void addOperation(String docId, EditOperation operation) {

        history
                .computeIfAbsent(docId, key -> new CopyOnWriteArrayList<>())
                .add(copyOperation(operation));
    }

    public static List<EditOperation> getOperationsSince(String docId, int versionExclusive) {
        List<EditOperation> operations = history.getOrDefault(docId, List.of());
        List<EditOperation> result = new ArrayList<>();

        for (EditOperation operation : operations) {
            if (operation.getVersion() > versionExclusive) {
                result.add(copyOperation(operation));
            }
        }

        return result;
    }

    private static EditOperation copyOperation(EditOperation source) {
        EditOperation operation = new EditOperation();
        operation.setType(source.getType());
        operation.setPosition(source.getPosition());
        operation.setText(source.getText());
        operation.setContent(source.getContent());
        operation.setTitle(source.getTitle());
        operation.setClientId(source.getClientId());
        operation.setOperationId(source.getOperationId());
        operation.setLength(source.getLength());
        operation.setDocumentId(source.getDocumentId());
        operation.setVersion(source.getVersion());
        operation.setTimestamp(source.getTimestamp());
        return operation;
    }
}
