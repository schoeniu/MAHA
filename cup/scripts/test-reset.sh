#!/bin/bash

#Restart deployment to purge queues
kubectl rollout restart deployment elasticmq -n cup
#Give it some head start so queues are available for other services to connect to
sleep 5
#Restart deployment to reset queue metrics
kubectl rollout restart deployment ext-request-proxy -n cup
kubectl rollout restart deployment cup-trigger -n cup
kubectl rollout restart deployment cup-process -n cup
kubectl rollout restart deployment cup-history -n cup
kubectl rollout restart deployment cup-cache -n cup
kubectl rollout restart deployment cup-vehicle-data -n cup
kubectl rollout restart deployment cup-rollout -n cup

kubectl rollout restart deployment maha -n cup

#Wait until ready
printf 'Waiting for cup-history to be ready...\n'
sleep 5

HTTPD=`curl -A "cup-history Check" -sL --connect-timeout 5 -w "%{http_code}\n" "http://localhost:30085/status/raw" -o /dev/null`
until [ "$HTTPD" == "200" ]; do
    printf 'Waiting for cup-history to be ready...\n'
    sleep 2
    HTTPD=`curl -A "cup-history Check" -sL --connect-timeout 5 -w "%{http_code}\n" "http://localhost:30085/status/raw" -o /dev/null`
done

sleep 1

#Clean history
curl http://localhost:30085/status/delete -X DELETE

printf 'Ready, waiting 5 more sec to stabilize...\n'

sleep 5

echo ''
cd ./logs

./tail.sh



curl http://localhost:30085/status/summary?frameLength=60000000