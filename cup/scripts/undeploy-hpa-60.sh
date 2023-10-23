#!/bin/bash
set -e

cd ./../k8s

echo "Undeploying hpa-60..."
kubectl delete -f ./hpas-60