#!/usr/bin/env bash
#
# Generates a small DEV-ONLY PKI for the mTLS code sample.
#
# Two CAs, mirroring the production split:
#
#   web-ca    stand-in for a PUBLIC CA (e.g. Let's Encrypt). Signs the
#             admin's browser-facing SERVER certificate only. In production
#             browsers would trust the public CA; here we trust web-ca.
#
#   mtls-ca   the INTERNAL machine CA. Signs the machine-identity
#             certificates used for mutual TLS:
#               - admin-mtls: the admin's CLIENT identity (CN=oc-admin)
#                             when it calls the worker
#               - worker:     the worker's SERVER certificate (CN=oc-worker)
#                             (machine-only endpoint: no public cert needed)
#
# The browser-facing and the machine-facing certificates are deliberately
# SEPARATE, as in production: different issuer, lifecycle, blast radius.
#
# DO NOT use these certificates (unencrypted keys, self-signed CAs) in
# production. See README.md.
set -euo pipefail

CERTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$CERTS_DIR"

DAYS=825

new_ca() { # name subject
  echo ">> $1 CA"
  openssl req -x509 -newkey rsa:4096 -nodes -days 3650 \
    -keyout "$1.key" -out "$1.crt" -subj "$2"
}

new_cert() { # name subject ca-key extensions
  echo ">> $1 certificate"
  openssl req -newkey rsa:2048 -nodes -keyout "$1.key" -out "$1.csr" -subj "$2"
  openssl x509 -req -in "$1.csr" -CA "$3.crt" -CAkey "$3.key" -CAcreateserial \
    -days "$DAYS" -out "$1.crt" -extfile <(printf "%s" "$4")
}

new_ca web-ca    "/CN=OC-Dev-Web-CA"
new_ca mtls-ca   "/CN=OC-Dev-Internal-CA"

# admin -> browser: server cert from the (dev) public CA, SAN for localhost
new_cert admin-web "/CN=oc-admin" web-ca \
  "subjectAltName=DNS:localhost
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
"

# admin -> worker: machine identity, client role only
new_cert admin-mtls "/CN=oc-admin" mtls-ca \
  "keyUsage=digitalSignature
extendedKeyUsage=clientAuth
"

# worker: machine-only endpoint; internal server cert (no public cert needed)
# (the localhost SAN is for the dev setup; in prod it would be an internal
# hostname)
new_cert worker "/CN=oc-worker" mtls-ca \
  "subjectAltName=DNS:localhost
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
"

echo ">> done, generated in $CERTS_DIR:"
ls -1
