<?php
require 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $email = $_POST['email'] ?? '';
    $password = $_POST['password'] ?? '';
    $confirm_password = $_POST['confirm_password'] ?? '';
    
    if (empty($email) || empty($password) || empty($confirm_password)) {
        echo json_encode([
            "status" => false,
            "message" => "Email, password, and confirmation are required."
        ]);
        exit;
    }
    
    if ($password !== $confirm_password) {
        echo json_encode([
            "status" => false,
            "message" => "Passwords do not match."
        ]);
        exit;
    }
    
    if (strlen($password) < 6) {
        echo json_encode([
            "status" => false,
            "message" => "Password must be at least 6 characters long."
        ]);
        exit;
    }
    
    // Check if user exists
    $check_user = $conn->prepare("SELECT id FROM users WHERE email = ?");
    $check_user->bind_param("s", $email);
    $check_user->execute();
    $user_result = $check_user->get_result();
    
    if ($user_result->num_rows === 0) {
        echo json_encode([
            "status" => false,
            "message" => "User not found."
        ]);
        $check_user->close();
        $conn->close();
        exit;
    }
    $check_user->close();
    
    // Hash the new password
    $hashed_password = password_hash($password, PASSWORD_DEFAULT);
    
    // Update password
    $stmt = $conn->prepare("UPDATE users SET password = ? WHERE email = ?");
    $stmt->bind_param("ss", $hashed_password, $email);
    
    if ($stmt->execute()) {
        echo json_encode([
            "status" => true,
            "message" => "Password updated successfully."
        ]);
    } else {
        echo json_encode([
            "status" => false,
            "message" => "Error updating password."
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
