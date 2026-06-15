#!/bin/env bash

for archetypeproject in *-archetype; do
  pushd "$archetypeproject"
  mvn clean install
  popd
done