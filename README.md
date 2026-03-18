# Opencast Quarkus

*-work in progress-*  
In this repo we will rewrite Opencast as [Quarkus](https://quarkus.io/) project and will also document the ongoing 
process and discuss required changes.

### The problem
- we currently can’t update to Spring Security 6 and need another solution (https://github.com/opencast/opencast/issues/6991)
  -  this is a problem bc there are open security issues that won’t be patched
  - Harvard made it possible to get to Spring Security 5
    - fixes most critical vulnerabilities, but that’s also out of support already
    - https://github.com/opencast/opencast/pull/7371
    - blocked bc CFX 4 implementation of Jakarta EE not available for OSGI
- We only use spring security in the kernel, and only a small part of it
- Updating jetty -> get rid if ops4j

### Why Quarkus  
- replace both Spring Security & Osgi: framework will change, but we can keep the modular structure 
- from RedHat
- implements jax-rs
- direct Jakarta EE impl. -> switch to other Jakarta EE impl. is easier

Advantages:
- good dev mode with hot reload
- starts dependencies as containers automatically
- quick startup times
- good documentation
- Kubernetes-native / create container images with the framework
- no application server needed

Disadvantages:
- doesn’t support as many auth methods as spring (see below)


There has already been a Quarkus Workshop: [Opencast Explore Quarkus Workshop](https://explore.opencast.org/webinars/v/D2c05bkb7EJ)
