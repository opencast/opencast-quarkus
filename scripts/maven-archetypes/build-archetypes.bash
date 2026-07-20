#!/usr/bin/env bash

cd "$(dirname "$BASH_SOURCE")"

for archetypeproject in *-archetype; do
  pushd "$archetypeproject"
  mvn clean install
  popd
done