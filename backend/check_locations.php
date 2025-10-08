<?php
require 'config.php';
header('Content-Type: application/json');

// Get all locations from database
$sql = "SELECT l.*, u.name as user_name, u.email FROM locations l 
        JOIN users u ON l.user_id = u.id 
        ORDER BY l.created_at DESC";

$result = $conn->query($sql);

if ($result && $result->num_rows > 0) {
    $locations = [];
    while ($row = $result->fetch_assoc()) {
        $locations[] = [
            'id' => $row['id'],
            'user_id' => $row['user_id'],
            'user_name' => $row['user_name'],
            'user_email' => $row['email'],
            'current_location' => $row['current_location'],
            'destination' => $row['destination'],
            'created_at' => $row['created_at']
        ];
    }
    
    echo json_encode([
        'status' => true,
        'count' => count($locations),
        'data' => $locations
    ]);
} else {
    echo json_encode([
        'status' => true,
        'count' => 0,
        'message' => 'No location data found',
        'data' => []
    ]);
}

$conn->close();
?>
