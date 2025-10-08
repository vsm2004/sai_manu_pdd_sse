<?php
require 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $user_message = $_POST['message'] ?? '';
    $user_id = $_POST['user_id'] ?? '';
    
    if (empty($user_message)) {
        echo json_encode([
            "status" => false,
            "message" => "Message is required."
        ]);
        exit;
    }
    
    // Simple AI responses for demo - replace with actual AI integration
    $responses = [
        'hello' => 'Hello! How can I help you with your travel plans?',
        'hi' => 'Hi there! What can I assist you with today?',
        'booking' => 'I can help you with booking hotels, resorts, and vacation packages. What are you looking for?',
        'help' => 'I can assist you with finding accommodations, booking stays, and answering travel questions.',
        'price' => 'Prices vary based on location, dates, and accommodation type. Would you like me to search for specific options?',
        'default' => 'Thank you for your message. Our AI is processing your request. How else can I help you today?'
    ];
    
    $message_lower = strtolower(trim($user_message));
    $ai_response = $responses['default'];
    
    foreach ($responses as $keyword => $response) {
        if ($keyword !== 'default' && strpos($message_lower, $keyword) !== false) {
            $ai_response = $response;
            break;
        }
    }
    
    // Log the conversation (optional)
    if (!empty($user_id)) {
        $stmt = $conn->prepare("INSERT INTO ai_conversations (user_id, user_message, ai_response, created_at) VALUES (?, ?, ?, NOW())");
        $stmt->bind_param("iss", $user_id, $user_message, $ai_response);
        $stmt->execute();
        if (isset($stmt)) $stmt->close();
    }
    
    echo json_encode([
        "status" => true,
        "message" => "Response generated successfully.",
        "ai_response" => $ai_response
    ]);
    
    $conn->close();
} else {
    echo json_encode([
        "status" => false,
        "message" => "Invalid request method."
    ]);
}
?>
