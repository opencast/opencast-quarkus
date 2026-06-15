This small project should demonstrate the project structure and Opencast specifics like multi-projects, multiple modules with endpoint definitions, multiple distributions…

I've decided to split the parent POM into two POM projects. The base POM, located in the root directory, should contain project-specific metadata such as the license, description, URL to the project, issue tracker, documentation, developers, and other non-technical metadata. The modules and profiles (see below) should also be defined there. The second POM, which we'll call the parent POM, should define technical aspects such as which versions to use for dependencies and the build configuration. In the regular modules, we can set the parent project as POM parent and define dependencies without version information or other dependency-specific configurations because these are defined in the parent.

The Maven universe renamed "modules" to "subprojects" because the term "module" conflicts with Java's use of the term. Let's adopt this change and refer to Maven modules as subprojects from now on.

The project structure should look like this:

```text
[root directory]
|
|-> Base project, parent is ./modules/parent
|
|-> [modules]
|   |
:   |-> [parent]
    |   |
    |   |-> Parent project
    |
    |-> [service A API]
    |   |
    |   |-> Service A API project, parent is ../parent
    |
    |-> [service A Impl]
    |   |
    |   |-> Service A Impl project, parent is ../parent
    |
    :
```

The Opencast build produces not only a compiled project, but also multiple assemblies. These assemblies are specific Opencast distributions for particular use cases. The Admin assembly manages the entire Opencast cluster and hosts user- and system-facing services, such as the Admin UI, AssetManager, and JobDispatcher. The Worker assembly is designed to handle processing jobs that probably produce a heavy load. We want to maintain this functionality and achieve the same semantics with Quarkus.

The name "assemblies" is specific to Karaf. I would like to call this a "distribution." In the end, we will create an Admin distribution, a Worker distribution, and so on.

We need a way to define Opencast distributions. But what is a distribution from a technical point of view? First, it is a definition of which modules should run on the server at runtime. Second, the service configuration may differ between distributions. In Karaf, we created these definitions in Karaf assemblies. We can replace these with Maven subprojects for each distribution, in which we define the necessary modules and subprojects, as well as update or overwrite specific configurations. Additionally, we can select which distribution to build with Maven profiles. If no profile is given during the build, the default profile will be selected, as it currently does with Karaf.

Let's expand our project structure to include distribution definitions.

```text
[root directory]
|
|-> Base project, parent is ./modules/parent
|
|-> [modules]
|   |
|   |-> [parent]
|   |   |
|   |   |-> Parent project
|   |
|   |-> [service A API]
|   |   |
|   |   |-> Service A API subproject, parent is ../parent
|   |
|   |-> [service A Impl]
|   |   |
|   |   |-> Service A Impl subproject, parent is ../parent
|   |
|   :
|
|-> [distributions]
|   |
:   |-> [admin]
    |   |
    |   |-> Opencast Admin distribution subproject
    |
    |-> [presentation]
    |   |
    |   |-> Opencast Presentation distribution subproject
    |
    :
```

This repository demonstrates the project structure and includes some subprojects. The projects are named according to their intended use cases. For example, the "common-service-api" project defines common services, and the projects with the "-impl" suffix are implementations of the services defined in the "api" projects. There are also distribution-specific services and implementations for demonstration purposes. The functionality of the services is not relevant for the demonstration, but they define web endpoints named for their use cases.

To build the entire project, run the command `mvn clean install`. This command calls the `mvn quarkus:build` command internally. If no profile is given, Maven will build the default one. In this case, the profile is named "allinone." The built project is located under `distributions/<profile name>/target/quarkus-app/`. The process for running the build is described below. To build a different distribution, select it by providing a profile name to the Maven command. For example, the command `mvn clean install -P admin,worker` will build the admin and worker distributions.

After building the project, change to the distribution build directory (`distributions/<profile name>/target/quarkus-app/`), then run the command: `java -jar quarkus-run.jar`. You can pass Quarkus-specific configurations as environment variables, such as `QUARKUS_HTTP_PORT=9000 java -jar quarkus-run.jar`, or by creating a configuration file (more on that later). All known Quarkus configurations are documented [here](https://quarkus.io/guides/all-config).

During development, you may want to take advantage of Quarkus's dev mode. In dev mode, you will receive a useful Quarkus developer web app. Hot-loading Java code and configurations will also save you a lot of time. To run Quarkus dev mode, build and run the project with `mvn quarkus:dev`. Maven profiles are also supported.