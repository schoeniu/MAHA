#!/bin/bash
set -e

AWS_REGION="eu-central-1"
AWS_ACCESS_KEY_ID="localAccessKey"
AWS_SECRET_ACCESS_KEY="localSecretKey"

# Configure AWS CLI with credentials and region
aws configure set aws_access_key_id $AWS_ACCESS_KEY_ID
aws configure set aws_secret_access_key $AWS_SECRET_ACCESS_KEY
aws configure set default.region $AWS_REGION

# Define queue names
list="\
EXT_REQUEST \
TRIGGER \
CACHE_REQUEST \
CACHE_RESPONSE \
VEHICLE_DATA_REQUEST \
VEHICLE_DATA_RESPONSE \
PROCESSED \
ROLLED_OUT \
HISTORY"

# Create SQS queues
for name in $list; do
    aws sqs create-queue --queue-name $name --endpoint-url http://localstack:4566 --attributes VisibilityTimeout=30
done

# Print final result
aws sqs list-queues --endpoint-url http://localstack:4566