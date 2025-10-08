<?php
require 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $destination = $_GET['destination'] ?? null;

    if ($destination) {
        $stmt = $conn->prepare("SELECT * FROM resorts WHERE location = ?");
        $stmt->bind_param("s", $destination);
    } else {
        $stmt = $conn->prepare("SELECT * FROM resorts LIMIT 5");
    }

    $stmt->execute();
    $result = $stmt->get_result();

    $resorts = [];
    while ($row = $result->fetch_assoc()) {
        $resorts[] = $row;
    }

    echo json_encode([
        "status" => true,
        "message" => count($resorts) . " resorts found",
        "data" => $resorts
    ]);

    $stmt->close();
    $conn->close();
}
?>
