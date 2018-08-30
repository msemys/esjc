#!/bin/bash

GIT_ROOT=$(git rev-parse --show-toplevel)
mkdir -p ${GIT_ROOT}/tools/ssl
pushd ${GIT_ROOT}/tools/ssl
openssl req -x509 -sha256 -nodes -days 365 -subj "/CN=localhost" -newkey rsa:2048 -keyout domain.pem -out domain.csr
openssl pkcs12 -passout "pass:" -export -inkey domain.pem -in domain.csr -out domain.p12
popd
