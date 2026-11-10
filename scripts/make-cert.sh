#!/usr/bin/env bash
# Make the local CA and the server certificate for HTTPS/PWA.
#
# The CA private key stays in KEYDIR and is never committed. The public CA certificate,
# the server certificate and the server key (DER) are copied into the app assets.
# Rerun this when the projector IP changes: the CA stays the same, so a phone that already
# trusts it does not need to install a new CA.
#
# Usage: scripts/make-cert.sh [ip]
#   KEYDIR=~/.local/android/projector-cert scripts/make-cert.sh 192.168.1.200

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IP="${1:-${DEV:-192.168.1.112}}"
IP="${IP%%:*}"                       # drop :5555 if DEV was used
KEYDIR="${KEYDIR:-$HOME/.local/android/projector-cert}"
OUT="$ROOT/panel/app/src/main/assets"
OPENSSL="${OPENSSL:-openssl}"

mkdir -p "$KEYDIR" "$OUT"

if [[ ! -f "$KEYDIR/ca.key" || ! -f "$KEYDIR/ca.crt" ]]; then
  echo "==> Creating a local CA in $KEYDIR"
  cat > "$KEYDIR/ca.cnf" <<'EOF'
[req]
distinguished_name = dn
x509_extensions = v3_ca
prompt = no
[dn]
CN = Projector Local CA
O = Projector
[v3_ca]
basicConstraints = critical,CA:TRUE
keyUsage = critical,keyCertSign,cRLSign
subjectKeyIdentifier = hash
EOF
  "$OPENSSL" req -x509 -newkey rsa:2048 -nodes -sha256 -days 3650 \
    -keyout "$KEYDIR/ca.key" -out "$KEYDIR/ca.crt" -config "$KEYDIR/ca.cnf"
fi

echo "==> Creating a server certificate for $IP"
"$OPENSSL" req -newkey rsa:2048 -nodes -sha256 \
  -keyout "$KEYDIR/server.key" -out "$KEYDIR/server.csr" -subj "/CN=Projector"

cat > "$KEYDIR/server.ext" <<EOF
basicConstraints = CA:FALSE
keyUsage = digitalSignature,keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = IP:$IP,IP:127.0.0.1,DNS:localhost,DNS:projector.local
EOF

"$OPENSSL" x509 -req -in "$KEYDIR/server.csr" \
  -CA "$KEYDIR/ca.crt" -CAkey "$KEYDIR/ca.key" -CAcreateserial \
  -out "$KEYDIR/server.crt" -days 3650 -sha256 -extfile "$KEYDIR/server.ext"

"$OPENSSL" pkcs8 -topk8 -nocrypt -in "$KEYDIR/server.key" \
  -outform DER -out "$OUT/server.key.der"
cp "$KEYDIR/server.crt" "$OUT/server.crt"
cp "$KEYDIR/ca.crt" "$OUT/ca.crt"

chmod 600 "$KEYDIR/ca.key" "$KEYDIR/server.key"
echo
echo "==> CA certificate (install this on the phone): $OUT/ca.crt"
"$OPENSSL" x509 -in "$OUT/ca.crt" -noout -fingerprint -sha256
echo "==> Server SAN:"
"$OPENSSL" x509 -in "$OUT/server.crt" -noout -ext subjectAltName
