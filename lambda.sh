#!/bin/sh
if [ ! -e lambda/secret.txt ]; then
  head -c 20 /dev/urandom | hexdump -e '"%02x"' > lambda/secret.txt
fi

cd lambda && zip -r ../lambda.zip * && cd ..
pwd
aws lambda upload-function \
--function-name QuarterHandler \
--function-zip ./lambda.zip \
--role arn:aws:iam::050602089425:role/lambda_dynamo \
--handler lambda_function.lambda_handler \
--runtime python2.7 \
--mode event

