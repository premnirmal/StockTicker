#!/usr/bin/env bash
#
# Derive the iOS app version from the latest git tag, mirroring the Android app
# (app/build.gradle.kts derives versionName/versionCode from
# `git describe --tags --abbrev=0`).
#
# Source this script before generating the Xcode project so XcodeGen can expand
# the `${MARKETING_VERSION}` / `${CURRENT_PROJECT_VERSION}` references in
# project.yml:
#
#   cd iosApp
#   source ./version.sh
#   xcodegen generate
#
# It exports two environment variables:
#   MARKETING_VERSION        -> CFBundleShortVersionString (the git tag, e.g. 4.0.0)
#   CURRENT_PROJECT_VERSION  -> CFBundleVersion (integer build number)
#
# Both are guaranteed non-empty (falling back to 1.0 / 1 when no tag is
# reachable) because an app extension whose CFBundleVersion resolves to an empty
# string fails to install.

# Resolve this script's directory so it works regardless of the current
# working directory (and whether sourced or executed).
_version_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"

# The latest reachable tag (same command Android uses). Empty if none/no git.
_version_name="$(git -C "$_version_script_dir" describe --tags --abbrev=0 2>/dev/null || true)"
if [ -z "$_version_name" ]; then
  _version_name="1.0"
fi

# Build number = major*100000000 + minor*100000 + patch, mirroring the Android
# versionCode in app/build.gradle.kts. Missing components default to 0.
_version_major="$(printf '%s' "$_version_name" | cut -d. -f1)"
_version_minor="$(printf '%s' "$_version_name" | cut -d. -f2)"
_version_patch="$(printf '%s' "$_version_name" | cut -d. -f3)"
_version_major="${_version_major:-1}"
_version_minor="${_version_minor:-0}"
_version_patch="${_version_patch:-0}"
_version_code=$(( _version_major * 100000000 + _version_minor * 100000 + _version_patch ))

export MARKETING_VERSION="$_version_name"
export CURRENT_PROJECT_VERSION="$_version_code"

echo "iOS version from git: MARKETING_VERSION=$MARKETING_VERSION CURRENT_PROJECT_VERSION=$CURRENT_PROJECT_VERSION"

unset _version_script_dir _version_name _version_major _version_minor _version_patch _version_code
