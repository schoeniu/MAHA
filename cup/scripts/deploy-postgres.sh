#!/bin/bash

cd ./../k8s/postgres
kubectl config set-context docker-desktop
kubectl config set-context --current --namespace=cup
echo "Deploying postgres..."
kubectl apply -k ./base