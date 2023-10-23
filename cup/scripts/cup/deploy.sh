#!/bin/bash
set -e

cd ./../../k8s

echo "Deploying ext-request-proxy..."
kubectl apply -f ./ext-request-proxy

echo "Deploying cup-trigger..."
kubectl apply -f ./cup-trigger

echo "Deploying cup-process..."
kubectl apply -f ./cup-process

echo "Deploying cup-cache..."
kubectl apply -f ./cup-cache

echo "Deploying cup-vehicle-data..."
kubectl apply -f ./cup-vehicle-data

echo "Deploying cup-rollout..."
kubectl apply -f ./cup-rollout

echo "Deploying cup-history..."
kubectl apply -f ./cup-history