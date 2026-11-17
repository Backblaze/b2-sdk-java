#!/usr/bin/env bash
# Verify against the named maintainers' registered keys, isolated from the runner's own keyring.
set -euo pipefail

tag=${1:?usage: verify-release-tag.sh TAG [MAINTAINERS_FILE]}
maintainers=${2:-.github/signing-maintainers}
enforce=${ENFORCE_SIGNED_TAGS:-false}

reject() {
  if [ "$enforce" = true ]; then
    printf 'HOLD: %s\n' "$1" >&2
    exit 1
  fi
  printf '::warning::%s (ENFORCE_SIGNED_TAGS is off)\n' "$1"
  exit 0
}

git check-ref-format "refs/tags/$tag" >/dev/null 2>&1 || reject 'Invalid release tag'
[ "$(git cat-file -t "refs/tags/$tag" 2>/dev/null || true)" = tag ] || reject 'Release tag must be annotated and signed'
[ -r "$maintainers" ] || reject 'Maintainer key registry is unavailable'

temporary=$(mktemp -d)
cleanup() {
  gpgconf --homedir "$temporary/gnupg" --kill all >/dev/null 2>&1 || true
  rm -rf "$temporary"
}
trap cleanup EXIT
mkdir -m 700 "$temporary/gnupg"
printf 'no-auto-key-retrieve\nauto-key-locate clear\n' > "$temporary/gnupg/gpg.conf"
export GNUPGHOME="$temporary/gnupg"
touch "$temporary/allowed_signers"

while IFS= read -r username || [ -n "$username" ]; do
  case "$username" in ''|'#'*) continue ;; esac
  [[ "$username" =~ ^[a-zA-Z0-9][a-zA-Z0-9-]*$ ]] || reject 'Invalid maintainer username'
  if curl --fail --silent --show-error --location --max-time 20 "https://github.com/$username.gpg" > "$temporary/key.gpg" 2>/dev/null; then
    if [ -s "$temporary/key.gpg" ] && command -v gpg >/dev/null; then
      gpg --batch --import "$temporary/key.gpg" >/dev/null 2>&1 || true
    fi
  fi
  curl --fail --silent --show-error --location --max-time 20 "https://github.com/$username.keys" > "$temporary/key.ssh" 2>/dev/null || true
  # Signing-only SSH keys are distinct from the authentication keys served by .keys.
  gh api --paginate "users/$username/ssh_signing_keys?per_page=100" --jq '.[].key' >> "$temporary/key.ssh" 2>/dev/null || true
  while IFS= read -r key; do
    case "$key" in
      ssh-*|ecdsa-*) printf '%s namespaces="git" %s\n' "$username" "$key" >> "$temporary/allowed_signers" ;;
    esac
  done < "$temporary/key.ssh"
done < "$maintainers"

if git -c gpg.ssh.allowedSignersFile="$temporary/allowed_signers" verify-tag -- "$tag" > "$temporary/verification" 2>&1; then
  printf 'release tag: verified against a registered maintainer key\n'
else
  reject 'Release tag signature not verified against registered maintainer keys; enroll keys before enforcement'
fi
