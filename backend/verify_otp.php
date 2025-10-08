<?php
include 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $email = $_POST['email'] ?? '';
    $otp = $_POST['otp'] ?? '';
    
    if (empty($email) || empty($otp)) {
        echo json_encode([
            "status" => false,
            "message" => "Email and OTP are required."
        ]);
        exit;
    }
    
    // Check if OTP exists and is valid
    $stmt = $conn->prepare("SELECT id, user_id, expires_at FROM password_reset_otps WHERE email = ? AND otp_code = ? AND expires_at > NOW() ORDER BY created_at DESC LIMIT 1");
    $stmt->bind_param("ss", $email, $otp);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows > 0) {
        $otp_data = $result->fetch_assoc();
        
        // Mark OTP as used
        $update_stmt = $conn->prepare("UPDATE password_reset_otps SET used = 1 WHERE id = ?");
        $update_stmt->bind_param("i", $otp_data['id']);
        $update_stmt->execute();
        $update_stmt->close();
        
        echo json_encode([
            "status" => true,
            "message" => "OTP verified successfully.",
            "user_id" => $otp_data['user_id']
        ]);
    } else {
        echo json_encode([
            "status" => false,
            "message" => "Invalid or expired OTP."
        ]);
    }
    
    $stmt->close();
} else {
    echo json_encode([
        "status" => false,
        "message" => "Invalid request method."
    ]);
}

$conn->close();
?>
