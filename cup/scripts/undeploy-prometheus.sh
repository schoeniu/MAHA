#!/bin/bash
set -e

echo "Undeploying prometheus..."
cd ./../k8s/prometheus

kubectl delete -f .
