#!/bin/sh

if [ "$#" -eq 0 ]; then
    echo "Usage: retry-maven.sh command [arguments...]" >&2
    exit 2
fi

log_file=$(mktemp) || exit 1
trap 'rm -f "$log_file"' 0

attempt=1
while [ "$attempt" -le 3 ]; do
    if "$@" > "$log_file" 2>&1; then
        cat "$log_file"
        exit 0
    else
        status=$?
    fi

    cat "$log_file"
    if ! grep -Eiq 'HTTP/1\.1 (429|5[0-9][0-9])|HTTP response code: (429|5[0-9][0-9])|status code: (429|5[0-9][0-9])|return code is: (429|5[0-9][0-9])|temporarily unavailable|temporary failure|timed out|connection reset|could not resolve|failed to fetch|bad address|unknownhost|unknown host' "$log_file"; then
        exit "$status"
    fi

    if [ "$attempt" -eq 3 ]; then
        exit "$status"
    fi

    echo "Transient Maven network failure (attempt $attempt/3); retrying."
    sleep "$((attempt * 5))"
    attempt=$((attempt + 1))
done

exit 1
