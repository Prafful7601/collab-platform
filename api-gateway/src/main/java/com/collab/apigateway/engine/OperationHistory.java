package com.collab.apigateway.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.collab.apigateway.model.EditOperation;

public class OperationHistory {

    private static final Map<String, List<EditOperation>> history = new HashMap<>();

    public static void addOperation(String docId, EditOperation op) {

        history
                .computeIfAbsent(docId, k -> new ArrayList<>())
                .add(op);
    }

    public static List<EditOperation> getHistory(String docId) {

        return history.getOrDefault(docId, new ArrayList<>());
    }
}