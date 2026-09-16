#!/bin/sh
set -eu

mkdir -p /app/logs
chown app:app /app/logs

exec setpriv --reuid=app --regid=app --init-groups \
    java -jar /app/app.jar
