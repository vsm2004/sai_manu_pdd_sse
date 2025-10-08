<?php
require 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $email = $_POST['email'] ?? '';
    $password = $_POST['password'] ?? '';
    $username = $_POST['username'] ?? '';
    $action = $_POST['action'] ?? '';

    // Basic validation
    if (empty($email) || empty($password) || empty($username)) {
        echo json_encode([
            "status" => false,
            "message" => "Email, password, and username are required."
        ]);
        exit;
    }

    // Hash the password for security
    $hashedPassword = password_hash($password, PASSWORD_DEFAULT);

    // Update user credentials in the database
    $stmt = $conn->prepare("UPDATE users SET password = ?, name = ? WHERE email = ?");
    $stmt->bind_param("sss", $hashedPassword, $username, $email);

    if ($stmt->execute()) {
        if ($stmt->affected_rows > 0) {
            echo json_encode([
                "status" => true,
                "message" => "New credentials saved successfully.",
                "user" => [
                    "email" => $email,
                    "name" => $username
                ]
            ]);
        } else {
            echo json_encode([
                "status" => false,
                "message" => "User not found or no changes made."
            ]);
        }
    } else {
        echo json_encode([
            "status" => false,
            "message" => "Error saving credentials: " . $stmt->error
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
