<?php
// ================= DEBUG & JSON SETUP =================
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json');  // Ensures Postman interprets response as JSON

include "config.php";  // DB connection

// ================= CHECK REQUEST =================
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(["status"=>"error","message"=>"Only POST requests allowed"]);
    exit;
}

// ================= READ INPUT =================
$email = $_POST['email'] ?? null;
$password = $_POST['password'] ?? null;
$confirm_password = $_POST['confirm_password'] ?? null;

// JSON body fallback if POST empty
if (!$email || !$password || !$confirm_password) {
    $data = json_decode(file_get_contents("php://input"), true);
    $email = $data['email'] ?? $email;
    $password = $data['password'] ?? $password;
    $confirm_password = $data['confirm_password'] ?? $confirm_password;
}

// ================= VALIDATION =================
if (!$email || !$password || !$confirm_password) {
    echo json_encode(["status" => "error", "message" => "Email, password, and confirm password are required"]);
    exit;
}

if ($password !== $confirm_password) {
    echo json_encode(["status" => "error", "message" => "Passwords do not match"]);
    exit;
}

if (strlen($password) < 6) {
    echo json_encode(["status" => "error", "message" => "Password must be at least 6 characters long"]);
    exit;
}

// ================= SQL ESCAPE =================
$email = mysqli_real_escape_string($conn, $email);
$password = mysqli_real_escape_string($conn, $password);

// ================= CHECK USER EXISTS =================
$check = mysqli_query($conn, "SELECT id FROM users WHERE email='$email'");
if (mysqli_num_rows($check) == 0) {
    echo json_encode(["status" => "error", "message" => "Email not registered"]);
    exit;
}

// ================= HASH & UPDATE PASSWORD =================
$password_hashed = password_hash($password, PASSWORD_DEFAULT);

$update = mysqli_query($conn, "UPDATE users SET password='$password_hashed', otp=NULL, otp_expiry=NULL WHERE email='$email'");
if (!$update) {
    echo json_encode(["status" => "error", "message" => "DB error: " . mysqli_error($conn)]);
    exit;
}

// ================= SUCCESS =================
echo json_encode(["status" => "success", "message" => "Password successfully updated"]);
exit;
?>
