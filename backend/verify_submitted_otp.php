<?php
require 'config.php';
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
    
    // Check if OTP exists and is valid (within 10 minutes)
    $stmt = $conn->prepare("SELECT id, otp, created_at FROM otp_verifications WHERE email = ? AND otp = ? AND created_at > DATE_SUB(NOW(), INTERVAL 10 MINUTE) ORDER BY created_at DESC LIMIT 1");
    $stmt->bind_param("ss", $email, $otp);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows > 0) {
        // OTP is valid - mark as used
        $otp_record = $result->fetch_assoc();
        $update_stmt = $conn->prepare("UPDATE otp_verifications SET used = 1 WHERE id = ?");
        $update_stmt->bind_param("i", $otp_record['id']);
        $update_stmt->execute();
        $update_stmt->close();
        
        echo json_encode([
            "status" => true,
            "message" => "OTP verified successfully.",
            "email" => $email
        ]);
    } else {
        echo json_encode([
            "status" => false,
            "message" => "Invalid or expired OTP."
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
