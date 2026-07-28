#!/bin/sh

set -eu

if [ -z "${HEAP_DUMP_EXIT_MARKER_PATH:-}" ]; then
	exec java -jar /app/app.jar
fi

set +e
java -jar /app/app.jar
exit_code=$?
set -e

mkdir -p "$(dirname "$HEAP_DUMP_EXIT_MARKER_PATH")"
printf '%s\n' "$exit_code" > "$HEAP_DUMP_EXIT_MARKER_PATH"

exit "$exit_code"
