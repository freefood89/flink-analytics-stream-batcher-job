#!/usr/bin/env bash

set -xe

export PUBSUB_EMULATOR_HOST=localhost:8085
export PUBSUB_PROJECT_ID=local

docker compose up pubsub -d

sleep 5

source scripts/venv/bin/activate
python scripts/publisher.py local create topic-1
python scripts/subscriber.py local create topic-1 sub-1

docker compose up
#python publisher.py local publish topic-1
