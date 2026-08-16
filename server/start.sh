#!/bin/sh
set -e

cd "$(dirname "$0")"

if [ ! -d node_modules ]; then
    npm install --omit=dev
fi

exec node server.js
