package com.collab.apigateway.engine;

import com.collab.apigateway.model.EditOperation;

public class OperationTransformer {

    public static EditOperation transform(EditOperation incoming, EditOperation existing) {
        if (incoming == null || existing == null) {
            return incoming;
        }

        if (isInsert(existing)) {
            transformAgainstInsert(incoming, existing);
            return incoming;
        }

        if (isDelete(existing)) {
            transformAgainstDelete(incoming, existing);
        }

        return incoming;
    }

    private static void transformAgainstInsert(EditOperation incoming, EditOperation existing) {
        int existingPosition = existing.getPosition();
        int insertedLength = textLength(existing.getText());

        if (isInsert(incoming)) {
            if (incoming.getPosition() > existingPosition || (
                    incoming.getPosition() == existingPosition && tieBreak(incoming, existing) > 0
            )) {
                incoming.setPosition(incoming.getPosition() + insertedLength);
            }
            return;
        }

        if (isDelete(incoming) && incoming.getPosition() >= existingPosition) {
            incoming.setPosition(incoming.getPosition() + insertedLength);
        }
    }

    private static void transformAgainstDelete(EditOperation incoming, EditOperation existing) {
        int existingStart = existing.getPosition();
        int existingEnd = existing.getPosition() + existing.getLength();

        if (isInsert(incoming)) {
            if (incoming.getPosition() > existingEnd) {
                incoming.setPosition(incoming.getPosition() - existing.getLength());
            } else if (incoming.getPosition() >= existingStart) {
                incoming.setPosition(existingStart);
            }
            return;
        }

        if (!isDelete(incoming)) {
            return;
        }

        int incomingStart = incoming.getPosition();
        int incomingEnd = incoming.getPosition() + incoming.getLength();

        if (incomingEnd <= existingStart) {
            return;
        }

        if (incomingStart >= existingEnd) {
            incoming.setPosition(incoming.getPosition() - existing.getLength());
            return;
        }

        int overlapStart = Math.max(incomingStart, existingStart);
        int overlapEnd = Math.min(incomingEnd, existingEnd);
        int overlapLength = Math.max(0, overlapEnd - overlapStart);

        if (incomingStart >= existingStart) {
            incoming.setPosition(existingStart);
        }

        incoming.setLength(Math.max(0, incoming.getLength() - overlapLength));
    }

    private static boolean isInsert(EditOperation operation) {
        return "insert".equals(operation.getType());
    }

    private static boolean isDelete(EditOperation operation) {
        return "delete".equals(operation.getType());
    }

    private static int textLength(String text) {
        return text != null ? text.length() : 0;
    }

    private static int tieBreak(EditOperation incoming, EditOperation existing) {
        String incomingKey = (incoming.getClientId() != null ? incoming.getClientId() : "")
                + ":" + (incoming.getOperationId() != null ? incoming.getOperationId() : "");
        String existingKey = (existing.getClientId() != null ? existing.getClientId() : "")
                + ":" + (existing.getOperationId() != null ? existing.getOperationId() : "");
        return incomingKey.compareTo(existingKey);
    }
}
