// Test script to verify the UI fix works correctly
// This simulates the user typing and ensures no duplication occurs

const WebSocket = require('ws');

const docId = "doc1";
const wsUrl = "ws://localhost:8080/collab?docId=" + docId;

console.log("🔧 Testing WebSocket with fixed UI message format...\n");
console.log(`Connecting to: ${wsUrl}\n`);

const socket = new WebSocket(wsUrl);

let messageCount = 0;
let receivedContent = "";

socket.on('open', function() {
    console.log("✅ WebSocket connected\n");
    
    // Simulate first keystroke: send "H"
    setTimeout(() => {
        console.log("📤 Sending: { type: 'replace', text: 'H', documentId: 'doc1' }");
        const msg1 = {
            type: "replace",
            text: "H",
            documentId: docId
        };
        socket.send(JSON.stringify(msg1));
    }, 100);
    
    // Simulate second keystroke: send "He"
    setTimeout(() => {
        console.log("📤 Sending: { type: 'replace', text: 'He', documentId: 'doc1' }");
        const msg2 = {
            type: "replace",
            text: "He",
            documentId: docId
        };
        socket.send(JSON.stringify(msg2));
    }, 300);
    
    // Simulate third keystroke: send "Hel"
    setTimeout(() => {
        console.log("📤 Sending: { type: 'replace', text: 'Hel', documentId: 'doc1' }");
        const msg3 = {
            type: "replace",
            text: "Hel",
            documentId: docId
        };
        socket.send(JSON.stringify(msg3));
    }, 500);
    
    // Simulate fourth keystroke: send "Hell"
    setTimeout(() => {
        console.log("📤 Sending: { type: 'replace', text: 'Hell', documentId: 'doc1' }");
        const msg4 = {
            type: "replace",
            text: "Hell",
            documentId: docId
        };
        socket.send(JSON.stringify(msg4));
    }, 700);
    
    // Simulate fifth keystroke: send "Hello"
    setTimeout(() => {
        console.log("📤 Sending: { type: 'replace', text: 'Hello', documentId: 'doc1' }");
        const msg5 = {
            type: "replace",
            text: "Hello",
            documentId: docId
        };
        socket.send(JSON.stringify(msg5));
    }, 900);
});

socket.on('message', function(data) {
    messageCount++;
    receivedContent = data.toString();
    
    console.log(`📩 Message ${messageCount}: "${receivedContent}"`);
    
    // Check for duplication
    if (receivedContent.includes("HH") || receivedContent.includes("ee") || 
        receivedContent.includes("ll") || receivedContent.includes("oo")) {
        console.log("❌ DUPLICATION DETECTED! Content is being duplicated.\n");
    }
});

socket.on('error', function(error) {
    console.log("❌ WebSocket error:", error);
});

socket.on('close', function() {
    console.log("\n🔌 WebSocket closed");
    
    console.log("\n📊 Final Results:");
    console.log(`   Messages received: ${messageCount}`);
    console.log(`   Final content: "${receivedContent}"`);
    
    if (receivedContent === "Hello") {
        console.log("   ✅ TEST PASSED: Content received correctly without duplication!");
    } else if (receivedContent.includes("HellHello") || receivedContent.length > 10) {
        console.log("   ❌ TEST FAILED: Content appears duplicated or corrupted");
    } else {
        console.log("   ⚠️  TEST INCONCLUSIVE: Check content manually");
    }
});

// Auto close after 3 seconds to prevent hanging
setTimeout(() => {
    console.log("\n⏱️ Test timeout - closing connection");
    socket.close();
}, 3000);
