#!/bin/bash
echo "Building MAHA..."
cd ./../services/maha

docker build -t maha:1.0 .