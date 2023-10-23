#!/bin/bash

echo ''
cd ./logs

./tail.sh



curl http://localhost:30085/status/summary?frameLength=60000000