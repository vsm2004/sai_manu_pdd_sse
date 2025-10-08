<?php
require 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $voice_input = $_POST['voice_input'] ?? '';
    $user_id = $_POST['user_id'] ?? '';
    
    if (empty($voice_input)) {
        echo json_encode([
            "status" => false,
            "message" => "Voice input is required."
        ]);
        exit;
    }
    
    // Simple voice processing for demo - replace with actual speech-to-text and AI
    $voice_responses = [
        'book hotel' => 'I found several hotels in your area. Would you like me to show you the options?',
        'find resort' => 'Here are some popular resorts. Which destination interests you?',
        'vacation package' => 'I can help you find vacation packages. What type of experience are you looking for?',
        'business trip' => 'For business accommodations, I recommend hotels with meeting facilities and WiFi.',
        'default' => 'I heard you say: "' . $voice_input . '". How can I help you with your travel needs?'
    ];
    
    $input_lower = strtolower(trim($voice_input));
    $voice_response = $voice_responses['default'];
    
    foreach ($voice_responses as $keyword => $response) {
        if ($keyword !== 'default' && strpos($input_lower, $keyword) !== false) {
            $voice_response = $response;
            break;
        }
    }
    
    // Log the voice interaction (optional)
    if (!empty($user_id)) {
        $stmt = $conn->prepare("INSERT INTO voice_interactions (user_id, voice_input, voice_response, created_at) VALUES (?, ?, ?, NOW())");
        $stmt->bind_param("iss", $user_id, $voice_input, $voice_response);
        $stmt->execute();
        if (isset($stmt)) $stmt->close();
    }
    
    echo json_encode([
        "status" => true,
        "message" => "Voice processed successfully.",
        "voice_response" => $voice_response,
        "recognized_text" => $voice_input
    ]);
    
    $conn->close();
} else {
    echo json_encode([
        "status" => false,
        "message" => "Invalid request method."
    ]);
}
?>
