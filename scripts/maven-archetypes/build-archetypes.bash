#!/usr/bin/env bash

cd "$(dirname "${BASH_SOURCE[0]}")" || exit

for archetypeproject in *-archetype; do
  pushd "$archetypeproject" || exit
  mvn clean install
  popd || exit
done
