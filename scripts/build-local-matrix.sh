#!/usr/bin/env bash
set -u -o pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_OUT="$ROOT/dist/local-matrix"
OUT_DIR="${OUT_DIR:-$DEFAULT_OUT}"
MAVEN_METADATA_URL="${MAVEN_METADATA_URL:-https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml}"

REPOS=(
  "$ROOT"
  "$(cd "$ROOT/../PleaseStore" && pwd)"
  "$(cd "$ROOT/../PleaseChop" && pwd)"
)

usage() {
  cat <<'EOF'
Usage:
  scripts/build-local-matrix.sh [--targets TARGETS] [--repo PATH ...] [--out DIR]

Builds local jars for the latest stable NeoForge releases in the Minecraft 1.21.x line.

Options:
  --targets TARGETS  Comma-separated minecraft:neoforge pairs.
                     Example: 1.21.1:21.1.224,1.21.11:21.11.42
                     Default: discover latest non-beta 21.x NeoForge releases from Maven.
  --repo PATH        Repo to build. Can be repeated. Defaults to PleaseSit, PleaseStore, PleaseChop.
  --out DIR          Output directory. Defaults to dist/local-matrix in PleaseSit.
  --help             Show this help.
EOF
}

TARGETS="${TARGETS:-}"
CUSTOM_REPOS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --targets)
      TARGETS="${2:-}"
      shift 2
      ;;
    --repo)
      CUSTOM_REPOS+=("$(cd "$2" && pwd)")
      shift 2
      ;;
    --out)
      OUT_DIR="$2"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ ${#CUSTOM_REPOS[@]} -gt 0 ]]; then
  REPOS=("${CUSTOM_REPOS[@]}")
fi

discover_targets() {
  python3 - "$MAVEN_METADATA_URL" <<'PY'
import re
import sys
import urllib.request

url = sys.argv[1]
xml = urllib.request.urlopen(url, timeout=30).read().decode()
versions = re.findall(r"<version>([^<]+)</version>", xml)
latest_by_minor = {}

def sort_key(version):
    return tuple(int(part) for part in version.split("."))

for version in versions:
    if "-beta" in version:
        continue
    match = re.fullmatch(r"21\.(\d+)\.(\d+)", version)
    if not match:
        continue
    minor = int(match.group(1))
    current = latest_by_minor.get(minor)
    if current is None or sort_key(version) > sort_key(current):
        latest_by_minor[minor] = version

for minor in sorted(latest_by_minor):
    print(f"1.21.{minor}:{latest_by_minor[minor]}")
PY
}

if [[ -z "$TARGETS" ]]; then
  echo "Discovering latest stable NeoForge 21.x releases..."
  TARGETS="$(discover_targets | paste -sd, -)"
fi

IFS=',' read -r -a TARGET_LIST <<< "$TARGETS"

mkdir -p "$OUT_DIR"

failures=()
successes=()

for repo in "${REPOS[@]}"; do
  if [[ ! -x "$repo/gradlew" ]]; then
    failures+=("$repo: missing executable gradlew")
    continue
  fi

  mod_name="$(grep -E '^mod_name=' "$repo/gradle.properties" | cut -d= -f2-)"
  mod_version="$(grep -E '^mod_version=' "$repo/gradle.properties" | cut -d= -f2-)"

  for target in "${TARGET_LIST[@]}"; do
    minecraft_version="${target%%:*}"
    neo_version="${target#*:}"
    neo_minor="$(echo "$neo_version" | cut -d. -f2)"
    neo_next_minor="$((neo_minor + 1))"
    minecraft_range="[$minecraft_version]"
    neo_range="[21.$neo_minor,21.$neo_next_minor)"
    safe_mod_name="$(echo "$mod_name" | tr '[:upper:] ' '[:lower:]-')"
    target_out="$OUT_DIR/$safe_mod_name/$minecraft_version"

    echo
    echo "== $mod_name $mod_version | Minecraft $minecraft_version | NeoForge $neo_version =="

    if (
      cd "$repo" &&
      ./gradlew clean build \
        -Pminecraft_version="$minecraft_version" \
        -Pneo_version="$neo_version" \
        -Pminecraft_version_range="$minecraft_range" \
        -Pneo_version_range="$neo_range" \
        -Puse_parchment_mappings=false
    ); then
      mkdir -p "$target_out"
      jar="$(find "$repo/build/libs" -maxdepth 1 -type f -name '*.jar' ! -name '*-sources.jar' | head -n 1)"
      if [[ -z "$jar" ]]; then
        failures+=("$mod_name $minecraft_version: build succeeded but no jar found")
        continue
      fi
      jar_base="$(basename "$jar" .jar)"
      dest="$target_out/${jar_base}-mc${minecraft_version}-neoforge${neo_version}.jar"
      cp "$jar" "$dest"
      successes+=("$dest")
    else
      failures+=("$mod_name $minecraft_version / NeoForge $neo_version")
    fi
  done
done

echo
echo "== Build summary =="
if [[ ${#successes[@]} -gt 0 ]]; then
  echo "Built jars:"
  printf '  %s\n' "${successes[@]}"
fi

if [[ ${#failures[@]} -gt 0 ]]; then
  echo
  echo "Failed targets:"
  printf '  %s\n' "${failures[@]}"
  exit 1
fi
