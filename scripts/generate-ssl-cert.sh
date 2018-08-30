#!/bin/bash

GIT_ROOT=$(git rev-parse --show-toplevel)
mkdir -p ${GIT_ROOT}/tools/ssl
pushd ${GIT_ROOT}/tools/ssl

# generate CA-signed certificate
openssl genrsa -out rootCA.key 2048
openssl req -x509 -sha256 -new -nodes -subj "/CN=root" -key rootCA.key -days 365 -out rootCA.crt
openssl genrsa -out domain.key 2048
openssl req -new -sha256 -nodes -key domain.key -subj "/CN=localhost" -out domain.csr
openssl x509 -req -in domain.csr -CA rootCA.crt -CAkey rootCA.key -CAcreateserial -out domain.crt -days 365 -sha256
openssl req -x509 -sha256 -nodes -days 365 -subj "/CN=localhost" -newkey rsa:2048 -keyout invalid.key -out invalid.crt
openssl pkcs12 -passout "pass:" -export -inkey domain.key -in domain.crt -out domain.p12
popd
