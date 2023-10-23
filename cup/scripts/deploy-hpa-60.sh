#!/bin/bash
set -e

cd ./../k8s

echo "Deploying hpa-60..."
kubectl apply -f ./hpas-60