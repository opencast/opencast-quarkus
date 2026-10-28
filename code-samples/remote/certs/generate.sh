#!/usr/bin/env bash
#
# Generates a small DEV-ONLY PKI for the mTLS code sample.
#
# Each distribution gets exactly ONE certificate (CN = its machine identity).
# The same certificate is used for both roles:
#   - as the SERVER certificate for that distribution's own HTTPS port
#   - as the CLIENT certificate when it calls the other distribution
# This mirrors real OpenCast, where calls flow in both directions.
#
# DO NOT use these certificates (unencrypted keys, self-signed CA) in
# production. See README.md.
set -euo pipefail

CERTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$CERTS_DIR"

DAYS=825

echo ">> CA"
openssl req -x509 -newkey rsa:4096 -nodes -days 3650 \
  -keyout ca.key -out ca.crt -subj "/CN=OC-Dev-CA"

for name in admin worker; do
  echo ">> ${name} certificate (server + client)"
  openssl req -newkey rsa:2048 -nodes -keyout "$name.key" -out "$name.csr" \
    -subj "/CN=oc-${name}"
  openssl x509 -req -in "$name.csr" -CA ca.crt -CAkey ca.key -CAcreateserial \
    -days "$DAYS" -out "$name.crt" \
    -extfile <(printf "subjectAltName=DNS:localhost,DNS:%s\nkeyUsage=digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth,clientAuth\n" "$name")
done

echo ">> done, generated in $CERTS_DIR:"
ls -1
