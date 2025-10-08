<?php
include 'config.php';
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
    
    // Validate email format
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        echo json_encode([
            "status" => false,
            "message" => "Invalid email format."
        ]);
        exit;
    }
    
    // Check if email exists in database
    $check = $conn->prepare("SELECT id, name FROM users WHERE email = ?");
    $check->bind_param("s", $email);
    $check->execute();
    $result = $check->get_result();
    
    if ($result->num_rows == 0) {
        echo json_encode([
            "status" => false,
            "message" => "Email not found in our system."
        ]);
        $check->close();
        $conn->close();
        exit;
    }
    
    $user = $result->fetch_assoc();
    $check->close();
    
    // Generate 6-digit OTP
    $otp = str_pad(random_int(0, 999999), 6, '0', STR_PAD_LEFT);
    
    // Store OTP in database with expiration (5 minutes)
    $expiry = date('Y-m-d H:i:s', time() + 300); // 5 minutes from now
    $stmt = $conn->prepare("INSERT INTO password_reset_otps (user_id, email, otp_code, expires_at) VALUES (?, ?, ?, ?)");
    $stmt->bind_param("isss", $user['id'], $email, $otp, $expiry);
    
    if ($stmt->execute()) {
        // In a real application, you would send email here
        // For demo purposes, we'll return the OTP in the response
        echo json_encode([
            "status" => true,
            "message" => "OTP sent to your email address.",
            "otp" => $otp, // Remove this in production
            "expires_in" => 300 // 5 minutes in seconds
        ]);
        
        // TODO: Implement actual email sending
        // Example using PHPMailer or similar:
        /*
        $mail = new PHPMailer();
        $mail->setFrom('noreply@yourdomain.com');
        $mail->addAddress($email);
        $mail->Subject = 'Password Reset OTP';
        $mail->Body = "Your OTP for password reset is: $otp\nThis OTP expires in 5 minutes.";
        $mail->send();
        */
    } else {
        echo json_encode([
            "status" => false,
            "message" => "Failed to send OTP. Please try again."
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
