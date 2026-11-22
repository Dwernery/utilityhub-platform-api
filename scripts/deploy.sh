#!/usr/bin/env bash
# Deploy UtilityHub on the server: the API (docker compose) and/or the web UI (nginx).
#
#   ./scripts/deploy.sh            # whichever changed since its last successful deploy
#   ./scripts/deploy.sh api        # the API, even if unchanged
#   ./scripts/deploy.sh web        # the web UI, even if unchanged
#   ./scripts/deploy.sh --force    # both, even if unchanged (e.g. after editing .env)
#
# Stops at the first problem. The last successfully deployed commit of each app is
# kept in $STATE_DIR, so a deploy that failed halfway is retried next time. Settings (override by exporting them):
#   API_DIR   this repo on the server            (default: the folder above this script)
#   UI_DIR    the web UI checkout                (default: UI_DIR in .env, else a sibling
#             folder named utilityhub-ui or utility-hub-ui)
#   WEB_ROOT  the folder nginx serves            (default: /var/www/utility-hub)
# Read from $API_DIR/.env: optionally SITE_HOST (e.g. example.com) to check the public
# site at the end. (The API's own settings in .env, such as OWNER_PASSWORD_HASH and
# SESSION_SECRET, are read by docker compose, not by this script.)

set -euo pipefail

API_DIR="${API_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
WEB_ROOT="${WEB_ROOT:-/var/www/utility-hub}"
SUDO="${SUDO-sudo}"           # set SUDO= to run without sudo (tests)
STATE_DIR="${STATE_DIR:-${XDG_STATE_HOME:-$HOME/.local/state}/utilityhub-deploy}"
KEEP_OLD_ASSETS_DAYS=7        # old JS/CSS stay this long, so open tabs keep working
API_URL="http://127.0.0.1:8080"

step() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }
fail() { printf '\n\033[31mDeploy failed: %s\033[0m\n' "$*" >&2; exit 1; }

# A value from the server's .env, without sourcing (and running) the whole file.
env_value() { grep -E "^$1=" "$API_DIR/.env" 2>/dev/null | tail -n 1 | cut -d= -f2-; }

# Where the web UI is checked out: UI_DIR from .env if set ("~/" allowed), otherwise
# a sibling folder with the GitHub repo name or the older local name.
find_ui_dir() {
  local configured dir
  configured="$(env_value UI_DIR)"
  if [ -n "$configured" ]; then
    echo "${configured/#\~/$HOME}"
    return
  fi
  for dir in "$API_DIR/../utilityhub-ui" "$API_DIR/../utility-hub-ui"; do
    if [ -d "$dir/.git" ]; then
      echo "$dir"
      return
    fi
  done
  echo "$API_DIR/../utilityhub-ui"
}
UI_DIR="${UI_DIR:-$(find_ui_dir)}"

pull() { # $1=label $2=checkout
  step "$1: pulling latest code"
  git -C "$2" pull --ff-only
}

# Has this checkout's current commit already been deployed successfully?
already_deployed() { # $1=app name $2=checkout
  [ "$(cat "$STATE_DIR/$1" 2>/dev/null)" = "$(git -C "$2" rev-parse HEAD)" ]
}

record_deployed() { # $1=app name $2=checkout
  mkdir -p "$STATE_DIR"
  git -C "$2" rev-parse HEAD >"$STATE_DIR/$1"
}

deploy_api() {
  step "API: rebuilding and restarting"
  (cd "$API_DIR" && docker compose up -d --build)

  step "API: waiting for it to answer"
  local status="" i
  for i in $(seq 1 60); do
    status="$(curl -s -o /dev/null -w '%{http_code}' "$API_URL/api/library/books" || true)"
    [ "$status" = "401" ] && break
    sleep 2
  done
  if [ "$status" != "401" ]; then
    (cd "$API_DIR" && docker compose logs --tail=40 api) || true
    fail "API didn't come up with sign-in required (last status: ${status:-none})"
  fi

  # An empty sign-in is refused as invalid (400), which shows the sign-in endpoint is
  # reachable without needing the password here.
  status="$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{}' \
    "$API_URL/api/auth/login")"
  [ "$status" = "400" ] || fail "The sign-in endpoint isn't answering as expected (status $status)"
  echo "API is up: no session → 401, sign-in endpoint → 400 for an empty request"
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
  local target="all" force=false arg
  for arg in "$@"; do
    case "$arg" in
      api | web | all) target="$arg" ;;
      --force) force=true ;;
      *) fail "usage: $0 [api|web|all] [--force]" ;;
    esac
  done
  # Naming an app deploys it even if unchanged; only "all" skips unchanged apps.
  [ "$target" = "all" ] || force=true

  # One deploy at a time.
  if command -v flock >/dev/null; then
    exec 9>/tmp/utilityhub-deploy.lock
    flock -n 9 || fail "another deploy is already running"
  fi

  if [ "$target" != "api" ]; then
    [ -d "$UI_DIR/.git" ] || fail "web UI checkout not found at $UI_DIR (set UI_DIR in .env)"
  fi

  if [ "$target" != "web" ]; then
    pull "API" "$API_DIR"
    if $force || ! already_deployed api "$API_DIR"; then
      deploy_api
      record_deployed api "$API_DIR"
    else
      echo "API: already deployed at $(git -C "$API_DIR" rev-parse --short HEAD), skipping"
    fi
  fi

  if [ "$target" != "api" ]; then
    pull "Web" "$UI_DIR"
    if $force || ! already_deployed web "$UI_DIR"; then
      deploy_web
      record_deployed web "$UI_DIR"
    else
      echo "Web: already deployed at $(git -C "$UI_DIR" rev-parse --short HEAD), skipping"
    fi
  fi

  step "Done"
}

# Run main unless the file is being sourced (the tests source it to call publish_web).
if [ "${BASH_SOURCE[0]}" = "$0" ]; then main "$@"; fi
