#!/bin/bash

while true; do
    kubectl top pods >> metrics.log  # Execute kubectl command and append output to metrics.log
    sleep 1  # Wait for 1 second before running the command again
done
