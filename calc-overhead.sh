#!/usr/bin/env bash
#
# FILE: calc-overhead.sh
#
# ABSTRACT: Calc overhead in bytes for JAR vs ZIP of presentation
#
# AUTHOR: Ralf Schandl
#
# CREATED: 2024-02-10
#

script_dir="$(cd "$(dirname "$0")" && pwd)" || exit 1

cd "$script_dir" || exit 1

zip_file="target/reveal.zip"

echo "Running maven build with reveal.js demo..."
if ! mvn clean package -Dmaven.test.skip=true -Pexample >/dev/null; then
    echo "maven build failed"
    exit 1
fi

echo "Zipping reveal.js demo..."
if ! ( cd src/main/example/presentation && zip -r "$script_dir/$zip_file" . -x jarp-metadata.properties > /dev/null ); then
    echo "zipping failed"
    exit 1
fi

zip_size="$(stat --format=%s "$zip_file")"

jar_size="$(stat --format=%s target/jar-presenter-*.jar)"

echo
printf "JAR Size: %'9d\n" "$jar_size"
printf "ZIP Size: %'9d\n" "$zip_size"
printf "          ---------\n"
printf "OVERHEAD: %'9d\n" $((jar_size - zip_size))

