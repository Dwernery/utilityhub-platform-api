#!/usr/bin/env bash
# Deploy UtilityHub on the server: the API (docker compose) and/or the web UI (nginx).
#
#   ./scripts/deploy.sh          # both
#   ./scripts/deploy.sh api      # only the API
#   ./scripts/deploy.sh web      # only the web UI
#
# Stops at the first problem. Settings (override by exporting them):
#   API_DIR   this repo on the server            (default: the folder above this script)
#   UI_DIR    the utility-hub-ui checkout        (default: $API_DIR/../utility-hub-ui)
#   WEB_ROOT  the folder nginx serves            (default: /var/www/utility-hub)
# Read from $API_DIR/.env: API_TOKEN (required for the API check) and, optionally,
# SITE_HOST (e.g. example.com) to check the public site at the end.

set -euo pipefail

API_DIR="${API_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
UI_DIR="${UI_DIR:-$API_DIR/../utility-hub-ui}"
WEB_ROOT="${WEB_ROOT:-/var/www/utility-hub}"
SUDO="${SUDO-sudo}"           # set SUDO= to run without sudo (tests)
KEEP_OLD_ASSETS_DAYS=7        # old JS/CSS stay this long, so open tabs keep working
API_URL="http://127.0.0.1:8080"

step() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }
fail() { printf '\n\033[31mDeploy failed: %s\033[0m\n' "$*" >&2; exit 1; }

# A value from the server's .env, without sourcing (and running) the whole file.
env_value() { grep -E "^$1=" "$API_DIR/.env" 2>/dev/null | tail -n 1 | cut -d= -f2-; }

deploy_api() {
  step "API: pulling latest code"
  git -C "$API_DIR" pull --ff-only

  step "API: rebuilding and restarting"
  (cd "$API_DIR" && docker compose up -d --build)

  local token
  token="$(env_value API_TOKEN)"
  [ -n "$token" ] || fail "API_TOKEN is missing from $API_DIR/.env"

  step "API: waiting for it to answer"
  local status="" i
  for i in $(seq 1 60); do
    status="$(curl -s -o /dev/null -w '%{http_code}' "$API_URL/api/library/books" || true)"
    [ "$status" = "401" ] && break
    sleep 2
  done
  if [ "$status" != "401" ]; then
    (cd "$API_DIR" && docker compose logs --tail=40 api) || true
    fail "API didn't come up with the token check on (last status: ${status:-none})"
  fi

  status="$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $token" "$API_URL/api/library/books")"
  [ "$status" = "200" ] || fail "API rejected the token from .env (status $status)"
  echo "API is up: no token → 401, token → 200"
  (cd "$API_DIR" && docker compose ps)
}

# Copies a built dist/ into WEB_ROOT without the site ever being empty or half-updated:
# new hashed assets go in next to the old ones, then index.html (which points at them)
# is swapped in one atomic rename. Assets from earlier builds are removed after
# KEEP_OLD_ASSETS_DAYS, so a tab left open across a deploy can still load them.
publish_web() {
  local dist="$1" root="$2"
  [ -f "$dist/index.html" ] || fail "no build found at $dist"

  $SUDO mkdir -p "$root/assets"
  $SUDO rsync -a "$dist/assets/" "$root/assets/"
  # Everything else (logo, icons, ...) except index.html, which goes last. These names
  # don't change between builds, so compare contents (--checksum): rsync's default
  # size-and-time check can skip a changed file of the same size.
  $SUDO rsync -a --checksum --exclude 'assets/' --exclude 'index.html' "$dist/" "$root/"
  $SUDO cp "$dist/index.html" "$root/.index.html.new"
  $SUDO mv -f "$root/.index.html.new" "$root/index.html"   # atomic on the same filesystem

  # Old assets: not part of this build and older than the grace period.
  local file name
  find "$root/assets" -type f -mtime "+$KEEP_OLD_ASSETS_DAYS" | while read -r file; do
    name="$(basename "$file")"
    [ -e "$dist/assets/$name" ] || $SUDO rm -f "$file"
  done

  if [ -n "$SUDO" ]; then $SUDO chown -R www-data:www-data "$root"; fi
}

deploy_web() {
  [ -d "$UI_DIR/.git" ] || fail "web UI checkout not found at $UI_DIR (set UI_DIR)"

  step "Web: pulling latest code"
  git -C "$UI_DIR" pull --ff-only

  step "Web: installing, testing and building"
  (cd "$UI_DIR" && npm ci --no-audit --no-fund && npm test && npm run build)

  step "Web: publishing to $WEB_ROOT"
  publish_web "$UI_DIR/dist" "$WEB_ROOT"

  local entry
  entry="$(grep -oE '/assets/index-[^"]+\.js' "$UI_DIR/dist/index.html" | head -n 1)"
  echo "Published build $entry"

  local host
  host="$(env_value SITE_HOST)"
  if [ -n "$host" ]; then
    step "Web: checking https://$host"
    curl -fsS "https://$host/" | grep -q "$entry" ||
      fail "the public site isn't serving the new build yet"
    [ "$(curl -s -o /dev/null -w '%{http_code}' "https://$host$entry")" = "200" ] ||
      fail "the new build's main file isn't reachable"
    echo "https://$host serves the new build"
  fi
}

main() {
  local target="${1:-all}"
  case "$target" in api | web | all) ;; *) fail "usage: $0 [api|web|all]" ;; esac

  # One deploy at a time.
  if command -v flock >/dev/null; then
    exec 9>/tmp/utilityhub-deploy.lock
    flock -n 9 || fail "another deploy is already running"
  fi

  case "$target" in
    api) deploy_api ;;
    web) deploy_web ;;
    all) deploy_api && deploy_web ;;
  esac
  step "Done"
}

# Run main unless the file is being sourced (the tests source it to call publish_web).
if [ "${BASH_SOURCE[0]}" = "$0" ]; then main "$@"; fi
