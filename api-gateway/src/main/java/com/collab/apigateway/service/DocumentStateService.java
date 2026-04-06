package com.collab.apigateway.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DocumentStateService {

    private final StringRedisTemplate redisTemplate;

    public DocumentStateService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String getDocument(String docId) {
        return redisTemplate.opsForValue().get("doc:" + docId);
    }

    public void saveDocument(String docId, String content) {
        redisTemplate.opsForValue().set("doc:" + docId, content);
    }
}