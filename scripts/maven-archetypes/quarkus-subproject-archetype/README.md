# Quarkus subproject archetype

## How to create a project from this archetype

Assuming you want to create a child project with given metadata.

- `groupId`: `org.opencastproject`
- `artifactId`: `quarkus-demo-api`
- `version`: `1.0-SNAPSHOT`

Please run this command inside modules subdirectory of the main project.

```
mvn archetype:generate                                   \
  -DarchetypeGroupId=org.opencastproject                 \
  -DarchetypeArtifactId=quarkus-subproject-archetype     \
  -DarchetypeVersion=1.0-SNAPSHOT                        \
  -DgroupId=org.opencastproject                          \
  -DmainProjectArtifactId=quarkus-demo                   \
  -DartifactId=quarkus-demo-api                          \
  -Dversion=1.0-SNAPSHOT
```

Yo may want to rename the created subproject directory.
It also should be linked as subproject in the main project pom.xml.