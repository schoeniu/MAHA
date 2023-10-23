#!/bin/bash

now=$(date +"%Y-%m-%dT%H-%M")

echo 'Saving logs in test-'$now

stern --max-log-requests 100 -n cup  -l logtail=true . > test-$now.log & stern --max-log-requests 100 -n cup  -l app=maha . > test-$now-maha.log

echo ''
echo 'Saved in '$now
echo ''

