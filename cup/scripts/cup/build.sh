#!/bin/bash
echo "Building cup..."
cd ./../../services/cup

docker build -t cup:1.0 .