package com.collab.apigateway.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.collab.apigateway.model.Document;

@Service
public class DocumentPersistenceClient {

    private final RestClient restClient;

    public DocumentPersistenceClient(
            RestClient.Builder restClientBuilder,
            @Value("${persistence.service.base-url:http://localhost:8081}") String persistenceServiceBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(persistenceServiceBaseUrl)
                .build();
    }

    public List<Document> listDocuments() {
        Document[] documents = restClient.get()
                .uri("/documents")
                .retrieve()
                .body(Document[].class);

        return documents != null ? Arrays.asList(documents) : List.of();
    }

    public Document getDocument(String documentId) {
        return restClient.get()
                .uri("/documents/{id}", documentId)
                .retrieve()
                .body(Document.class);
    }

    public Document saveDocument(Document document) {
        if (document.getId() == null || document.getId().isBlank()) {
            return restClient.post()
                    .uri("/documents")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(document)
                    .retrieve()
                    .body(Document.class);
        }

        return restClient.put()
                .uri("/documents/{id}", document.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(document)
                .retrieve()
                .body(Document.class);
    }
}
