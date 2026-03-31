#!/bin/sh
rm -rf ./clients

rm -rf ./docs/openapi.json

openapi-generator-cli generate -i ./docs/api.yaml -g java -o ./clients/

openapi-generator-cli generate -i ./docs/api.yaml -g openapi -o ./docs

rm -rf ./clients/.openapi-*
rm -rf ./clients/.git*
rm -rf ./clients/git*

rm -rf ./docs/.openapi-*
rm -rf ./docs/.git*
rm -rf ./docs/git*
rm -rf ./docs/README.md