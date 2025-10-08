<?php
require 'config.php';
header('Content-Type: application/json');

if ($_SERVER["REQUEST_METHOD"] === "POST") {
    $user_id = $_POST['user_id'] ?? '';
    $email = trim($_POST['email'] ?? '');
    $source_address = trim($_POST['current_location'] ?? '');
    $destination_address = trim($_POST['destination'] ?? '');

    // Debug logging
    error_log("enteraddress.php - user_id: '$user_id', email: '$email', source: '$source_address', dest: '$destination_address'");
    
    // Require addresses
    if (empty($source_address) || empty($destination_address)) {
        echo json_encode([
            "status" => false,
            "message" => "Current location and destination are required."
        ]);
        exit;
    }
    
    // Require at least one identifier (user_id OR email)
    if ((empty($user_id) || $user_id === '0') && empty($email)) {
        echo json_encode([
            "status" => false,
            "message" => "Either user_id or email is required."
        ]);
        exit;
    }

    // Resolve user_id by email if not provided or zero
    if (empty($user_id) || $user_id === '0') {
        if (!empty($email)) {
            $stmt = $conn->prepare("SELECT id FROM users WHERE email = ? LIMIT 1");
            $stmt->bind_param("s", $email);
            if ($stmt->execute()) {
                $res = $stmt->get_result();
                if ($row = $res->fetch_assoc()) {
                    $user_id = (string)$row['id'];
                }
            }
            $stmt->close();
        }
        
        // If still no user_id, generate one from hash of email
        if (empty($user_id) || $user_id === '0') {
            $user_id = abs(crc32($email));
        }
    }

    // Insert into locations
    $stmt = $conn->prepare("INSERT INTO locations (user_id, current_location, destination) VALUES (?, ?, ?)");
    $uid = intval($user_id);
    $stmt->bind_param("iss", $uid, $source_address, $destination_address);

    if ($stmt->execute()) {
        echo json_encode([
            "status" => true,
            "message" => "Location data saved successfully."
        ]);
    } else {
        echo json_encode([
            "status" => false,
            "message" => "Error saving data: " . $stmt->error
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
