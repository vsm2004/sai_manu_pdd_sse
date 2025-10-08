<?php
require 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $destination = $_GET['destination'] ?? null;

    if ($destination) {
        $stmt = $conn->prepare("SELECT * FROM business_stays WHERE location = ?");
        $stmt->bind_param("s", $destination);
    } else {
        $stmt = $conn->prepare("SELECT * FROM business_stays LIMIT 5");
    }

    $stmt->execute();
    $result = $stmt->get_result();

    $businessStays = [];
    while ($row = $result->fetch_assoc()) {
        $businessStays[] = $row;
    }

    echo json_encode([
        "status" => true,
        "message" => count($businessStays) . " business stays found",
        "data" => $businessStays
    ]);

    $stmt->close();
    $conn->close();
}
?>
