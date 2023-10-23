#!/bin/bash
set -e

echo "Undeploying grafana..."
cd ./../k8s/grafana

kubectl delete -k ./base
