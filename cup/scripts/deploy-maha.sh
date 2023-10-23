#!/bin/bash
set -e

cd ./../k8s

echo "Deploying MAHA..."
kubectl apply -f ./maha