#!/usr/bin/env bash
# Generates an RSA-2048 key pair for JWT RS256 signing and prints the
# Base64-encoded DER values ready to paste into APP_JWT_PRIVATE_KEY / APP_JWT_PUBLIC_KEY.
set -euo pipefail

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

openssl genrsa -out "$TMP/private.pem" 2048 2>/dev/null
openssl pkcs8 -topk8 -inform PEM -outform DER -in "$TMP/private.pem" -out "$TMP/private.der" -nocrypt
openssl rsa -in "$TMP/private.pem" -pubout -outform DER -out "$TMP/public.der" 2>/dev/null

PRIVATE=$(base64 -w 0 "$TMP/private.der")
PUBLIC=$(base64 -w 0 "$TMP/public.der")

echo "# Add these to your .env file or export them before running the app / docker compose"
echo ""
echo "APP_JWT_PRIVATE_KEY=$PRIVATE"
echo "APP_JWT_PUBLIC_KEY=$PUBLIC"
