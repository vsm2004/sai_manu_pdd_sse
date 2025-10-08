<?php
include 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $email = $_POST['email'] ?? '';
    $new_password = $_POST['new_password'] ?? '';
    $confirm_password = $_POST['confirm_password'] ?? '';
    
    if (empty($email) || empty($new_password) || empty($confirm_password)) {
        echo json_encode([
            "status" => false,
            "message" => "Email, new password and confirm password are required."
        ]);
        exit;
    }
    
    if ($new_password !== $confirm_password) {
        echo json_encode([
            "status" => false,
            "message" => "Passwords do not match."
        ]);
        exit;
    }
    
    if (strlen($new_password) < 6) {
        echo json_encode([
            "status" => false,
            "message" => "Password must be at least 6 characters long."
        ]);
        exit;
    }
    
    // Check if user exists
    $check = $conn->prepare("SELECT id FROM users WHERE email = ?");
    $check->bind_param("s", $email);
    $check->execute();
    $result = $check->get_result();
    
    if ($result->num_rows == 0) {
        echo json_encode([
            "status" => false,
            "message" => "User not found."
        ]);
        $check->close();
        $conn->close();
        exit;
    }
    
    $user = $result->fetch_assoc();
    $check->close();
    
    // Hash the new password
    $hashed_password = password_hash($new_password, PASSWORD_DEFAULT);
    
    // Update password
    $update = $conn->prepare("UPDATE users SET password = ?, updated_at = NOW() WHERE id = ?");
    $update->bind_param("si", $hashed_password, $user['id']);
    
    if ($update->execute()) {
        // Clear any pending OTPs for this user
        $clear_otps = $conn->prepare("UPDATE password_reset_otps SET used = 1 WHERE user_id = ?");
        $clear_otps->bind_param("i", $user['id']);
        $clear_otps->execute();
        $clear_otps->close();
        
        echo json_encode([
            "status" => true,
            "message" => "Password reset successfully."
        ]);
    } else {
        echo json_encode([
            "status" => false,
            "message" => "Failed to reset password. Please try again."
        ]);
    }
    
    $update->close();
} else {
    echo json_encode([
        "status" => false,
        "message" => "Invalid request method."
    ]);
}

$conn->close();
?>
