#!/usr/bin/env bash
# Prints the bcrypt hash of a password, for OWNER_PASSWORD_HASH in the server's .env:
#
#   ./scripts/hash-password.sh
#
# Asks for the password without showing it, so it stays out of your shell history.
# Uses htpasswd (built into macOS; apache2-utils on Linux), or Docker when it is missing.
set -euo pipefail

read -r -s -p "Password: " password; echo >&2
read -r -s -p "Again: " again; echo >&2
[ "$password" = "$again" ] || { echo "The passwords don't match." >&2; exit 1; }
[ ${#password} -ge 12 ] || { echo "Use at least 12 characters." >&2; exit 1; }

if command -v htpasswd >/dev/null; then
  hash="$(htpasswd -inBC 12 "" <<<"$password")"
else
  hash="$(docker run --rm -i httpd:2.4-alpine htpasswd -inBC 12 "" <<<"$password")"
fi
hash="${hash#:}"
hash="$(printf '%s' "$hash" | tr -d '\r\n')"

echo "Add this line to .env (keep the single quotes):" >&2
echo "OWNER_PASSWORD_HASH='$hash'"
