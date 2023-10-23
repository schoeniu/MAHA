#!/bin/bash
set -e

echo "Deploying prometheus..."
cd ./../k8s/prometheus

kubectl apply -f .
