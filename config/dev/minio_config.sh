#!/bin/sh

mc alias set storage "$ENDPOINT" "$(cat $ACCESS_KEY_FILE)" "$(cat $SECRET_KEY_FILE)"

mc admin config set storage/ notify_amqp:s3-raw-file-uploaded \
  url="amqp://$(cat $AMQP_USER_FILE):$(cat $AMQP_PASSWORD_FILE)@$AMQP_HOST" \
  exchange="amq.direct" \
  durable="on" \
  exchange_type="direct" \
  routing_key="s3-raw-file-uploaded" \
  comment="Destination for new raw file events"

mc event add storage/raw arn:minio:sqs::s3-raw-file-uploaded:amqp \
  --event put --prefix uploaded

mc admin service restart storage