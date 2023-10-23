#!/bin/bash

echo "Redeploying CUP..."
./undeploy.sh
./build.sh
./deploy.sh