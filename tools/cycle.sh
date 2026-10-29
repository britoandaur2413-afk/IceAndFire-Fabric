#!/bin/sh
# compile, apply site fixes, compile again; print the summary
cd "$(dirname "$0")/.."
python -I tools/fix_text.py >/dev/null
python -I tools/fix_overrides.py >/dev/null
./gradlew compileJava --no-daemon -q > build/compile.log 2>&1
python -I tools/fix_sites.py build/compile.log
./gradlew compileJava --no-daemon -q > build/compile.log 2>&1
python -I tools/errors.py build/compile.log | head -${1:-40}
