This small project should demonstrate the project structure and Opencast specifics like multi-projects, multiple modules with endpoint definitions, multiple distributions…

I've decided to split parent pom into two pom projects. In the base pom, placed in the root directory, we can define project specific metadata like license, description, URL to the project, issue tracker, documentation, developers and other non-technical metadata. But also the modules and profiles (see below) should be defined there. The second pom, lets call it parent pom, should define all the technical things like which versions should be used for the dependencies and build configuration. In the regular modules we can set the parent project as pom parent and are able to define dependencies without version information or other dependency specific configuration because this is done in the parent.

Maven universe renamed modules to subprojects, as the name module conflicts with the Java world modules. Let's adopt this change and call maven modules subprojects from now on.

The project structure should look like this

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

The output of the Opencast build is not only a compiled project but multiple assemblies. These assemblies are specific distributions of Opencast for specific use cases. Admin assembly is intended to manage the whole Opencast cluster but also host user and system facing services like Admin UI, Assetmanager, Jobdispatcher,…. The Worker assembly is designed for handling (video) processing jobs, that probably produces heavy load. We want to keep this and achieve same semantic with Quarkus as well.

The name assemblies is Karaf specific. I would like to call this distribution. At the end we will create Admin distribution, Worker distribution,…

We need a way to define Opencast distributions. But what is a distribution from technical point of view? In first place this is a definition which modules should run on the server at runtime. In second place the service configuration may differ between distributions. In Karaf we made thees definitions in Karaf assemblies. We can replace it with Maven subprojects for each distribution where we define the needed modules/subprojects and update/overwrite specific configurations. In addition to that we can select which distribution should be built with maven profiles. Without giving a profile during build, the default profile should be selected, like it does currently with Karaf.

Lets extend our project structure for distribution definitions

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

This repository demonstrates the project structure with some subprojects. They are named for intended use cases like common-service-api is a project defining common services, the -impl suffixed projects are implementations of the services defined in api projects. Distribution specific services and implementation are also there for demonstration reasons. What the services are doing is not relevant for the demonstration, but they define web endpoints named for the use cases.

To build the whole project, run `mvn clean install`. This command will call `mvn quarkus:build` internally. Without giving any profile, Maven will build the default one. In this case it is `allinone`. The built project is located under `distributions/<profile name>/target/quarkus-app/`. How to run the build is described below. To build a different distribution you can select it by giving a profile name(s) to the Maven command. The command `mvn clean install -P admin,worker` will build the admin and worker distribution.

To run the project after it was built, change to the distribution build directory `distributions/<profile name>/target/quarkus-app/` and run `java -jar quarkus-run.jar`. You can pass Quarkus specific configuration as Environment variables like `QUARKUS_HTTP_PORT=9000 java -jar quarkus-run.jar` or creating a configuration file (more on that later). All known Quarkus configurations are documented [here](https://quarkus.io/guides/all-config).

During development, you may want to benefit from the Quarkus dev mode. In dev mode you will get a useful Quarkus developer web app. But also hot-loading of Java code and configuration save you a lot of time. To run Quarkus dev mode build and run the project with `mvn quarkus:dev`. Maven profiles are supported here too.