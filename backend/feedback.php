<?php
require 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $user_id = $_POST['user_id'] ?? '';
    $booking_id = $_POST['booking_id'] ?? '';
    $rating = $_POST['rating'] ?? '';
    $feedback_text = $_POST['feedback'] ?? '';
    $hotel_name = $_POST['hotel_name'] ?? '';
    
    // Basic validation
    if (empty($user_id) || empty($rating) || empty($feedback_text)) {
        echo json_encode([
            "status" => false,
            "message" => "User ID, rating, and feedback are required."
        ]);
        exit;
    }
    
    // Validate rating is between 1-5
    if (!is_numeric($rating) || $rating < 1 || $rating > 5) {
        echo json_encode([
            "status" => false,
            "message" => "Rating must be between 1 and 5."
        ]);
        exit;
    }
    
    // Insert feedback record
    $stmt = $conn->prepare("INSERT INTO feedback (user_id, booking_id, hotel_name, rating, feedback_text, created_at) VALUES (?, ?, ?, ?, ?, NOW())");
    $stmt->bind_param("issis", $user_id, $booking_id, $hotel_name, $rating, $feedback_text);
    
    if ($stmt->execute()) {
        echo json_encode([
            "status" => true,
            "message" => "Feedback submitted successfully.",
            "feedback_id" => $conn->insert_id
        ]);
    } else {
        echo json_encode([
            "status" => false,
            "message" => "Error submitting feedback: " . $stmt->error
        ]);
    }
    
    $stmt->close();
    $conn->close();
} else {
    echo json_encode([
        "status" => false,
        "message" => "Invalid request method."
    ]);
}
?>
