#!/bin/bash
echo "Running testnetworkserver.sh"
sleep 2

echo -e "HTTP/1.1 200 OK\n\n Hello Network Test" | nc -l 0.0.0.0 10001
