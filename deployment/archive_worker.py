"""Consume SNS events delivered through SQS and archive immutable event IDs in private S3 storage."""
import json
import os
import re
import boto3

s3 = boto3.client('s3')

def handler(event, context):
    """Failures retry the batch; repeated event IDs overwrite the same key rather than creating distinct records."""
    for record in event['Records']:
        message = json.loads(record['body'])
        event_id = message['eventId']
        if not re.fullmatch(r'[a-f0-9-]{36}', event_id):
            raise ValueError('Invalid event ID')
        s3.put_object(Bucket=os.environ['ARCHIVE_BUCKET'], Key='audit/events/' + event_id + '.json',
                      Body=record['body'].encode('utf-8'), ContentType='application/json', ServerSideEncryption='AES256')
    return {'archived': len(event['Records'])}
