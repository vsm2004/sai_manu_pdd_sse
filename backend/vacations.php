<?php
require 'config.php';
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $destination = $_GET['destination'] ?? null;

    if ($destination) {
        $stmt = $conn->prepare("SELECT * FROM tour_agencies WHERE location = ?");
        $stmt->bind_param("s", $destination);
    } else {
        $stmt = $conn->prepare("SELECT * FROM tour_agencies LIMIT 5");
    }

    $stmt->execute();
    $result = $stmt->get_result();

    $agencies = [];
    while ($row = $result->fetch_assoc()) {
        $agencies[] = $row;
    }

    echo json_encode([
        "status" => true,
        "message" => count($agencies) . " tour agencies found",
        "data" => $agencies
    ]);

    $stmt->close();
    $conn->close();
}
?>
