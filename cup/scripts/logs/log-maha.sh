#!/bin/bash

now=$(date +"%Y-%m-%dT%H-%M-%S")

echo 'Saving logs in' $now

until [ "0" == "1" ]; do
    kubectl logs --namespace cup --selector=app=maha -f > $now.log
    sleep 5
done