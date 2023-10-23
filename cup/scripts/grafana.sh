#!/bin/bash
set -e

echo "Deploying grafana..."
cd ./../k8s/grafana

kubectl apply -k ./base
