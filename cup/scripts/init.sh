#!/bin/bash
set -e

./deploy-elasticmq.sh

./deploy-postgres.sh

./deploy-dashboard.sh

./deploy-metricsserver.sh

./deploy-kubestatemetrics.sh

./prometheus.sh

./grafana.sh

cd cup

./build.sh

./deploy.sh

cd ..

./deploy-maha.sh