
from __future__ import print_function

import boto3
import sys
import hashlib
import botocore.exceptions
import json
import requests

print('Loading function')

with open('secret.txt', 'r') as f:
    secret = f.read()

def hash_for(info):
    return hashlib.sha256(info['email'] + info['name'] + secret).hexdigest()

def lambda_handler(event, context):
    print("Received event: " + json.dumps(event, indent=2))
    shake_prefix = 'shakedowns/'

    operation = event['operation']

    s3 = boto3.resource('s3')
    bucket = s3.Bucket('quarter-state')

    res = {}
    if operation == 'login':
        url = "https://kencoder.auth0.com/tokeninfo"
        token = event['token']
        r = requests.post(url, data = {"id_token": token})
        r.raise_for_status()
        info = r.json()
        user = {}
        for k in ['picture', 'name', 'email']:
            user[k] = info[k]
        user['hash'] = hash_for(user)
        res['user'] = user
        res['status'] = 'ok'
    else:
        user = event['user']
        res = {'user': user}
        if user['hash'] != hash_for(user):
            raise Exception("Invalid security")

        if operation == 'gear':
            r = requests.get("https://docs.google.com/spreadsheets/d/1pJUBDAWn6qBsSIU-SHnI6gHnprbKPPtrubU_toHvWJw/pub?output=tsv")
            res['expected-gear'] = r.text
            res['status'] = 'ok'
        elif operation == 'store':
            bucket.put_object(Key= shake_prefix + event['file'], Body=event['body'])
            res['status'] = 'ok'
        elif operation == 'get':
            try:
                # TODO figure out specific error
                txt = s3.Object(bucket_name='quarter-state', key=shake_prefix + event['file']).get()['Body'].read()
                res['shakedown'] = txt
            except:
    #            res['errorMessage'] = str(sys.exc_info()[0])
    #            return res
                pass
            res['status'] = 'ok'
        elif operation == 'list':
            rs = bucket.objects.filter(Prefix=shake_prefix)
            res['shakedowns'] = [f.key[len(shake_prefix):] for f in rs]
            res['status'] = 'ok'

        else:
            raise Exception("Unknown operation " + operation)

    return res
