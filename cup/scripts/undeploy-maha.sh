#!/bin/bash
set -e

cd ./../k8s

echo "Undeploying MAHA..."
kubectl delete -f ./maha