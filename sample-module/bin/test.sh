#!/bin/bash
echo "Running test script"
sleep 2
if [ "$1" -eq "pass" ]
then
  exit 0
fi
if [ "$1" -eq "fail" ]
then
  exit 1
fi