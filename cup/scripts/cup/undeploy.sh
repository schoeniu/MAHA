#!/bin/bash

cd ./../../k8s

echo "Undeploy ext-request-proxy..."
kubectl delete -f ./ext-request-proxy

echo "Undeploy cup-trigger..."
kubectl delete -f ./cup-trigger

echo "Undeploy cup-process..."
kubectl delete -f ./cup-process

echo "Undeploy cup-cache..."
kubectl delete -f ./cup-cache

echo "Undeploy cup-vehicle-data..."
kubectl delete -f ./cup-vehicle-data

echo "Undeploy cup-rollout..."
kubectl delete -f ./cup-rollout

echo "Undeploy cup-history..."
kubectl delete -f ./cup-history