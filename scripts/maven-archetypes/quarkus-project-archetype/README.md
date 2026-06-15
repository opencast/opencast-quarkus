# Quarkus project archetype

## How to create a project from this archetype

Assuming you want to create a project with given metadata.

- `groupId`: `org.opencastproject`
- `artifactId`: `quarkus-demo`
- `version`: `1.0-SNAPSHOT`

Please run this command.

```
mvn archetype:generate                                          \
  -DarchetypeGroupId=org.opencastproject                        \
  -DarchetypeArtifactId=quarkus-project-archetype               \
  -DarchetypeVersion=1.0-SNAPSHOT                               \
  -DgroupId=org.opencastproject                                 \
  -DartifactId=quarkus-demo                                     \
  -Dversion=1.0-SNAPSHOT
```
