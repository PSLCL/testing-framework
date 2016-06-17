#!/bin/bash
echo "Running testnetworkserver.sh"
sleep 2

echo -e "HTTP/1.1 200 OK\n\n Hello Network Test" | nc -l localhost 10001
