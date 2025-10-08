<?php
include 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $name = $_POST['name'] ?? '';
    $email = $_POST['email'] ?? '';
    $password = $_POST['password'] ?? '';
    
    // Basic validation
    if (empty($name) || empty($email) || empty($password)) {
        echo json_encode([
            "status" => false,
            "message" => "Name, email and password are required."
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
    
    // Check if email already exists
    $check = $conn->prepare("SELECT id FROM users WHERE email = ?");
    $check->bind_param("s", $email);
    $check->execute();
    $check->store_result();
    
    if ($check->num_rows > 0) {
        echo json_encode([
            "status" => false,
            "message" => "Email already registered.",
            "email_exists" => true
        ]);
        $check->close();
        $conn->close();
        exit;
    }
    
    // Hash the password
    $hashed_password = password_hash($password, PASSWORD_DEFAULT);
    
    // Insert new user with hashed password
    $insert = $conn->prepare("INSERT INTO users (name, email, password) VALUES (?, ?, ?)");
    $insert->bind_param("sss", $name, $email, $hashed_password);
    
    if ($insert->execute()) {
        $user_id = $conn->insert_id;
        echo json_encode([
            "status" => true,
            "message" => "Registration successful.",
            "user" => [
                "id" => $user_id,
                "name" => $name,
                "email" => $email
            ]
        ]);
    } else {
        echo json_encode([
            "status" => false,
            "message" => "Error during registration."
        ]);
    }
    
    $check->close();
    $insert->close();
} else {
    echo json_encode([
        "status" => false,
        "message" => "Invalid request method."
    ]);
}

$conn->close();
?>
