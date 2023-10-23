#!/bin/bash
set -e

echo "Deploying infrastructure..."
kubectl config set-context docker-desktop
kubectl config set-context --current --namespace=cup
cd ./../k8s/localstack

echo "Building dockerfile"
docker build --tag cuplocalstack:1.0 .

echo "Deploying"
kubectl apply -f .

echo "Deployment finished"
