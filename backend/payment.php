<?php
require 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $booking_id = $_POST['booking_id'] ?? '';
    $user_id = $_POST['user_id'] ?? '';
    $amount = $_POST['amount'] ?? '';
    $payment_method = $_POST['payment_method'] ?? $_POST['method'] ?? '';
    $hotel_name = $_POST['hotel_name'] ?? '';
    $place_type = $_POST['place_type'] ?? '';
    
    // Basic validation
    if (empty($booking_id) || empty($user_id) || empty($amount) || empty($payment_method)) {
        echo json_encode([
            "status" => false,
            "message" => "All payment fields are required."
        ]);
        exit;
    }
    
    // Validate amount is numeric
    if (!is_numeric($amount) || floatval($amount) <= 0) {
        echo json_encode([
            "status" => false,
            "message" => "Invalid amount."
        ]);
        exit;
    }
    
    // Generate a transaction ID
    $transaction_id = 'TXN' . time() . rand(1000, 9999);
    
    // Insert payment record
    $stmt = $conn->prepare("INSERT INTO payments (booking_id, user_id, amount, payment_method, transaction_id, hotel_name, place_type, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'completed')");
    $stmt->bind_param("sidssss", $booking_id, $user_id, $amount, $payment_method, $transaction_id, $hotel_name, $place_type);
    
    if ($stmt->execute()) {
        echo json_encode([
            "status" => true,
            "message" => "Payment processed successfully.",
            "transaction_id" => $transaction_id
        ]);
    } else {
        echo json_encode([
            "status" => false,
            "message" => "Error processing payment: " . $stmt->error
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
