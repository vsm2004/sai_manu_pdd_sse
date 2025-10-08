<?php
require 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $user_id = $_POST['user_id'] ?? '';
    $selection = $_POST['selection'] ?? '';
    $destination = $_POST['destination'] ?? '';

    // Basic validation
    if (empty($user_id) || empty($selection)) {
        echo json_encode([
            "status" => false,
            "message" => "User ID and selection are required."
        ]);
        exit;
    }

    // Optional: set a default destination
    if (empty($destination)) {
        $destination = 'Athens';
    }

    // Insert selection into DB
    $stmt = $conn->prepare("INSERT INTO dashboard_selections (user_id, selection) VALUES (?, ?)");
    $stmt->bind_param("is", $user_id, $selection);

    if ($stmt->execute()) {
        // Map selection to redirect page
        $redirect_map = [
            'hotel' => 'hotels.php',
            'resort' => 'resorts.php',
            'vacation' => 'vacations.php',
            'business' => 'businesstrips.php'
        ];
        $redirect = $redirect_map[strtolower($selection)] ?? 'dashboard.php';

        // Call Flask API
        $flask_url = $GLOBALS['FLASK_API_BASE'] . "/search?destination=" . urlencode($destination);

        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $flask_url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);
        curl_setopt($ch, CURLOPT_TIMEOUT, 10);
        curl_setopt($ch, CURLOPT_HEADER, true); // Include headers to check HTTP code
        curl_setopt($ch, CURLOPT_NOBODY, false);

        $flask_response = curl_exec($ch);
        $curl_error = curl_error($ch);
        $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        $flask_data = null;
        $flask_error = null;

        if ($flask_response === false || $http_code !== 200) {
            $flask_error = $curl_error ?: "HTTP Error Code: $http_code";
        } else {
            $flask_data = json_decode($flask_response, true);
        }

        echo json_encode([
            "status" => true,
            "message" => "Selection recorded.",
            "redirect" => $redirect,
            "flask_data" => $flask_data,
            "flask_error" => $flask_error
        ]);
    } else {
        echo json_encode([
            "status" => false,
            "message" => "Failed to save selection."
        ]);
    }

    $stmt->close();
    $conn->close();
}
?>
