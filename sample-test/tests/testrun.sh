#!/bin/bash
echo "Running testrun script"
sleep 2
if [ "$1" = "pass" ]
then
  echo "Pass"
  exit 0
fi
if [ "$1" = "fail" ]
then
  echo "Fail"
  exit 1
fi