#!/bin/bash
set -e

echo "Deploying elasticmq..."
kubectl config set-context docker-desktop
kubectl config set-context --current --namespace=cup
cd ./../k8s/elasticmq

echo "Building dockerfile"
docker build --tag elasticmq:1.0 .

echo "Deploying"
kubectl apply -f .

echo "Deployment finished"
