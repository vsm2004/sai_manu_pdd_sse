<?php
require 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $user_id = $_POST['user_id'] ?? '';
    $hotel_name = $_POST['hotel_name'] ?? '';
    $place_type = $_POST['place_type'] ?? '';
    $check_in = $_POST['check_in'] ?? '';
    $check_out = $_POST['check_out'] ?? '';
    $guests = $_POST['guests'] ?? 1;
    $rooms = $_POST['rooms'] ?? 1;
    $total_price = $_POST['total_price'] ?? '';
    
    // Basic validation
    if (empty($user_id) || empty($hotel_name) || empty($total_price)) {
        echo json_encode([
            "status" => false,
            "message" => "User ID, hotel name, and total price are required."
        ]);
        exit;
    }
    
    // Generate booking ID
    $booking_id = 'BK' . time() . rand(1000, 9999);
    
    // Insert booking record
    $stmt = $conn->prepare("INSERT INTO bookings (booking_id, user_id, hotel_name, place_type, check_in, check_out, guests, rooms, total_price, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', NOW())");
    $stmt->bind_param("sisssiids", $booking_id, $user_id, $hotel_name, $place_type, $check_in, $check_out, $guests, $rooms, $total_price);
    
    if ($stmt->execute()) {
        echo json_encode([
            "status" => true,
            "message" => "Booking created successfully.",
            "booking_id" => $booking_id,
            "hotel_name" => $hotel_name,
            "total_price" => $total_price
        ]);
    } else {
        echo json_encode([
            "status" => false,
            "message" => "Error creating booking: " . $stmt->error
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
