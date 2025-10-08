<?php
require 'config.php';
header('Content-Type: application/json; charset=utf-8');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Detect JSON input
    $input = file_get_contents('php://input');
    $data = json_decode($input, true) ?: $_POST;

    $user_id = $data['user_id'] ?? '';
    $rating = $data['rating'] ?? '';
    $emoji = $data['emoji'] ?? '';
    $feedback_text = $data['feedback_text'] ?? '';
    $destination = $data['destination'] ?? 'Athens';

    if (empty($user_id) || empty($feedback_text)) {
        echo json_encode([
            "status" => false,
            "message" => "User ID and feedback are required."
        ]);
        exit;
    }

    $flask_url = $GLOBALS['FLASK_API_BASE'] . "/analyze-feedback";

    $payload = json_encode([
        "user_id" => $user_id,
        "rating" => $rating,
        "emoji" => $emoji,
        "feedback_text" => $feedback_text,
        "destination" => $destination
    ]);

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $flask_url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);

    $flask_response = curl_exec($ch);
    $curl_error = curl_error($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    $analysis = null;
    $flask_error = null;

    if ($flask_response === false || $http_code !== 200) {
        $flask_error = $curl_error ?: "HTTP Error Code: $http_code";
    } else {
        $analysis = json_decode($flask_response, true);
    }

    echo json_encode([
        "status" => true,
        "message" => "Feedback submitted.",
        "analysis" => $analysis,
        "flask_error" => $flask_error
    ]);
}
?>
