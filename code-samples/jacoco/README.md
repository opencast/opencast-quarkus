# JaCoCo Integration Example

This project demonstrates JaCoCo (Java Code Coverage) integration\
with a Maven multi-module Quarkus project.

The project was generated using the `quarkus-project-archetype` from this repository,
with an additional module `hello` created via the `quarkus-subproject-archetype` archetype.

## Notable Configuration

The `hello` module demonstrates requiring 100% coverage across all JaCoCo counters.
See `modules/hello/pom.xml` for the plugin configuration.

## Generating a Coverage Report

Run the build (which executes tests with the JaCoCo agent):

```sh
mvn clean verify
```

**Note**: The `clean` step is sometimes necessary to remove old coverage data!

Then generate the report:

```sh
mvn -pl modules/hello jacoco:report
```

The HTML report will be at `modules/hello/target/site/jacoco/index.html`.

## A note on integration tests

When you look at instructions for integrating JaCoCo,
you often find another Maven goal `jacoco:prepare-agent-integration`
to be used for integration tests.
Specifically, this means tests run by `failsave-plugin`,
as opposed to unit-tests, run by `surefire-plugin`.

What this essentially does is specify a different target file for the coverage data.
This makes report generation and coverage requirements like the one mentioned above more complicated,
which is why we opted out of this approach.
The way Quarkus runs tests, `prepare-agent` is enough to cover both unit and integration tests,

Getting coverage data from `QuarkusIntegrationTest`-s with JaCoCo **doesn't work at all**,
since those run in a completely different process, which might not even be a JVM.
