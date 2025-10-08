<?php
include "config.php";  // DB connection

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $email = null;

    // Try normal POST
    if (isset($_POST['email'])) $email = $_POST['email'];

    // Try JSON
    if (!$email) {
        $data = json_decode(file_get_contents("php://input"), true);
        if (isset($data['email'])) $email = $data['email'];
    }

    if (!$email) {
        echo json_encode(["status" => "error", "message" => "Email required"]);
        exit;
    }

    $email = mysqli_real_escape_string($conn, $email);

    // Check if user exists
    $check = mysqli_query($conn, "SELECT id FROM users WHERE email='$email'");
    if (mysqli_num_rows($check) == 0) {
        echo json_encode(["status" => "error", "message" => "Email not registered"]);
        exit;
    }

    // Generate new OTP
    $otp = rand(100000, 999999);
    $expiry = date("Y-m-d H:i:s", strtotime("+10 minutes"));

    // Update DB
    $update = mysqli_query($conn, "UPDATE users SET otp='$otp', otp_expiry='$expiry' WHERE email='$email'");
    if (!$update) {
        echo json_encode(["status" => "error", "message" => "DB error: " . mysqli_error($conn)]);
        exit;
    }

    // Send OTP again (PHPMailer or mail)
    // mail($email, "Your New OTP", "Your new OTP is: $otp");

    echo json_encode([
        "status" => "success",
        "message" => "New OTP sent to email"
        // For debugging (remove in production): "otp" => $otp
    ]);
} else {
    echo json_encode(["status" => "error", "message" => "Only POST requests allowed"]);
}
?>
