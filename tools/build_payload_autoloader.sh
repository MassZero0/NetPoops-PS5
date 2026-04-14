#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
SRC_DIR="${PROJECT_DIR}/src"
RES_DIR="${PROJECT_DIR}/resources"
BUILD_DIR="${TMPDIR:-/tmp}/poops_autoloader_build"
MAIN_CLASS="org.homebrew.Poops"

WITH_AUTOLOADER_JAR="${PROJECT_DIR}/payload_with_autoloader.jar"
WITHOUT_AUTOLOADER_JAR="${PROJECT_DIR}/payload.jar"
BUILD_BOTH=1

if [ "$#" -gt 0 ]; then
  case "$1" in
    --with-autoloader)
      BUILD_BOTH=0
      WITH_AUTOLOADER_JAR="${2:-${WITH_AUTOLOADER_JAR}}"
      ;;
    --without-autoloader)
      BUILD_BOTH=0
      WITHOUT_AUTOLOADER_JAR="${2:-${WITHOUT_AUTOLOADER_JAR}}"
      ;;
    *)
      BUILD_BOTH=0
      WITH_AUTOLOADER_JAR="$1"
      ;;
  esac
fi

require_tool() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required tool: $1" >&2
    exit 1
  fi
}

require_tool javac
require_tool jar
require_tool unzip
require_tool sha256sum

rm -rf "${BUILD_DIR}"

build_variant() {
  variant_name="$1"
  out_jar="$2"
  send_helper_elfs="$3"

  variant_dir="${BUILD_DIR}/${variant_name}"
  src_copy="${variant_dir}/src"
  classes_dir="${variant_dir}/classes"
  res_copy="${variant_dir}/resources"

  mkdir -p "${src_copy}" "${classes_dir}" "${res_copy}"
  mkdir -p "$(dirname "${out_jar}")"
  cp -R "${SRC_DIR}/." "${src_copy}/"
  cp -R "${RES_DIR}/." "${res_copy}/"

  if [ "${send_helper_elfs}" = "false" ]; then
    sed -i 's/private static final boolean SEND_HELPER_ELFS = true;/private static final boolean SEND_HELPER_ELFS = false;/' \
      "${src_copy}/org/homebrew/Poops.java"
    rm -f "${res_copy}/ps5_autoload.elf" "${res_copy}/ps5_killdiscplayer.elf"
  fi

  java_sources=(
    "${src_copy}/org/bdj/api/API.java"
    "${src_copy}/org/bdj/api/Buffer.java"
    "${src_copy}/org/bdj/api/Int8.java"
    "${src_copy}/org/bdj/api/Int32.java"
    "${src_copy}/org/bdj/api/Int32Array.java"
    "${src_copy}/org/bdj/api/Int64.java"
    "${src_copy}/org/bdj/api/KernelAPI.java"
    "${src_copy}/org/bdj/RemoteLogger.java"
    "${src_copy}/org/bdj/Screen.java"
    "${src_copy}/org/bdj/Status.java"
    "${src_copy}/org/homebrew/Poops.java"
  )

  echo "[build] Compiling ${variant_name} (${MAIN_CLASS}) for Java 8 bytecode..."
  javac -source 8 -target 8 -cp "${src_copy}" -d "${classes_dir}" "${java_sources[@]}"

  echo "[build] Creating ${out_jar}..."
  jar cfe "${out_jar}" "${MAIN_CLASS}" -C "${classes_dir}" org -C "${res_copy}" .

  echo "[build] Verifying ${out_jar}..."
  unzip -t "${out_jar}" >/dev/null

  echo "[build] OK: ${out_jar}"
  sha256sum "${out_jar}"
}

if [ "${BUILD_BOTH}" -eq 1 ]; then
  build_variant "with-autoloader" "${WITH_AUTOLOADER_JAR}" "true"
  build_variant "without-autoloader" "${WITHOUT_AUTOLOADER_JAR}" "false"
elif [ "${1:-}" = "--without-autoloader" ]; then
  build_variant "without-autoloader" "${WITHOUT_AUTOLOADER_JAR}" "false"
else
  build_variant "with-autoloader" "${WITH_AUTOLOADER_JAR}" "true"
fi
