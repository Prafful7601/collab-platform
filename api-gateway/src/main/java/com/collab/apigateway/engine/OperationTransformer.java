package com.collab.apigateway.engine;

import com.collab.apigateway.model.EditOperation;

public class OperationTransformer {

    public static EditOperation transform(EditOperation incoming, EditOperation existing) {

        if ("insert".equals(existing.getType()) && "insert".equals(incoming.getType())) {

            if (incoming.getPosition() >= existing.getPosition()) {

                incoming.setPosition(
                        incoming.getPosition() + existing.getText().length()
                );
            }
        }

        if ("delete".equals(existing.getType()) && "insert".equals(incoming.getType())) {

            if (incoming.getPosition() > existing.getPosition()) {

                incoming.setPosition(
                        incoming.getPosition() - existing.getLength()
                );
            }
        }

        return incoming;
    }
}