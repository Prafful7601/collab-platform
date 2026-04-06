#!/usr/bin/env node
const WebSocket = require('ws');
const http = require('http');

// Test: Send an edit operation via WebSocket
const ws = new WebSocket('ws://localhost:8080/collab?docId=test-doc');

ws.on('open', () => {
  console.log('✓ WebSocket connected to API Gateway');
  
  // Send a test edit operation
  const operation = {
    type: 'insert',
    position: 0,
    text: 'Hello collaborative world!',
    documentId: 'test-doc'
  };
  
  ws.send(JSON.stringify(operation));
  console.log('✓ Sent edit operation:', operation.text);
});

ws.on('message', (data) => {
  console.log('✓ Received update from server:', data);
  ws.close();
});

ws.on('error', (err) => {
  console.error('✗ WebSocket error:', err.message);
  process.exit(1);
});

ws.on('close', () => {
  console.log('✓ Test completed successfully');
  process.exit(0);
});

// Timeout after 5 seconds
setTimeout(() => {
  console.error('✗ Test timed out');
  process.exit(1);
}, 5000);
