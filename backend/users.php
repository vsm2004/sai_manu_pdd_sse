<?php
require 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $email = $_GET['email'] ?? '';

    if (empty($email)) {
        echo json_encode([
            "status" => false,
            "message" => "Email is required."
        ]);
        exit;
    }

    $stmt = $conn->prepare("SELECT id, name, email FROM users WHERE email = ? LIMIT 1");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($row = $result->fetch_assoc()) {
        echo json_encode([
            "status" => true,
            "user" => $row
        ]);
    } else {
        echo json_encode([
            "status" => false,
            "message" => "User not found"
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


