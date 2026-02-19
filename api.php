<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

$file = __DIR__ . '/apis.txt';

if (!file_exists($file)) {
    echo json_encode(['error' => 'apis.txt not found']);
    exit;
}

$lines = file($file, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
$apis = [];

foreach ($lines as $line) {
    $line = trim($line);
    if (empty($line) || strpos($line, '|') === false) {
        continue;
    }
    
    list($name, $url) = explode('|', $line, 2);
    $apis[] = [
        'name' => trim($name),
        'url' => trim($url)
    ];
}

echo json_encode($apis, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
