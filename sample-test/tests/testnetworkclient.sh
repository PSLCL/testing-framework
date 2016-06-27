#!/bin/bash
echo "Running testnetworkclient.sh"

for i in `seq 1 20`
do
  sleep 5
  echo "HTTP Request $i"
  result=$(curl --write-out %{http_code} --silent --output /dev/null $1:10001)
  echo "HTTP Response: $result"
  if [ $result = 200 ]
  then
    echo "Pass"
    exit 0
  fi
done

echo "Fail"
exit 1