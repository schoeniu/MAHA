#!/bin/bash
set -e

echo "Redeploying MAHA..."
./undeploy-maha.sh
./build-maha.sh
./deploy-maha.sh