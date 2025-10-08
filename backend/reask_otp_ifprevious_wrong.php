<?php
require 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $email = $_POST['email'] ?? '';
    
    if (empty($email)) {
        echo json_encode([
            "status" => false,
            "message" => "Email is required."
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
            "message" => "Email not found."
        ]);
        $check_user->close();
        $conn->close();
        exit;
    }
    $check_user->close();
    
    // Generate new 6-digit OTP
    $otp = sprintf("%06d", rand(0, 999999));
    
    // Store new OTP in database (invalidates previous ones)
    $stmt = $conn->prepare("INSERT INTO otp_verifications (email, otp, created_at) VALUES (?, ?, NOW())");
    $stmt->bind_param("ss", $email, $otp);
    
    if ($stmt->execute()) {
        // In a real app, you would send the OTP via email/SMS
        echo json_encode([
            "status" => true,
            "message" => "New OTP sent to your email.",
            "otp" => $otp  // Remove this in production!
        ]);
    } else {
        echo json_encode([
            "status" => false,
            "message" => "Error sending new OTP."
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
