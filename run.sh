#!/usr/bin/env sh
set -e
cd "$(dirname "$0")"

# JDK 24+ (JEP 498): Maven ships Guava that still touches sun.misc.Unsafe; this quiets startup noise.
java_spec=$(java -XshowSettings:properties -version 2>&1 | awk -F' = ' '/java.specification.version = / {print $2; exit}')
if [ -n "$java_spec" ]; then
  major=${java_spec%%.*}
  if [ "$major" -ge 24 ] 2>/dev/null; then
    export MAVEN_OPTS="${MAVEN_OPTS:+$MAVEN_OPTS }--sun-misc-unsafe-memory-access=allow"
  fi
fi

if [ -x ./mvnw ]; then
  exec ./mvnw -q compile exec:java
fi
if command -v mvn >/dev/null 2>&1; then
  exec mvn -q compile exec:java
fi
echo "Maven not found. Use ./mvnw (recommended) or install mvn." >&2
exit 1
