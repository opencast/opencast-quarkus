# multitenancy

This project uses the [quarkus-multitenancy](https://github.com/mathias82/quarkus-multitenancy) extension form 
mathias82, which is in the process of becoming an official [quarkiverse extension](https://github.com/quarkiverse/quarkus-multitenancy).  

All parts that I just copied from the quarkus-multitenancy demo project are marked by a comment. 

### quarkus-multitenancy extension
The quarkus-multitenancy extension provides a consistent tenant resolution in quarkus, e.g. via a HTTP-Header.
Hereby the extension lets us implement (if needed) our custom:
- TenantContext
- TenantResolver
- TenantDataSourceRegistry
- TenantResolutionContext
- ...

The extension itself comes with a filter that resolves the tenant if there is no authorization needed.  
The extension supports a database-per-tenant approach.

### Tenant resolution with authorization
In order to resolve the tenant when making an authorization request it is needed to implement a custom 
HTTPAuthenticationMechanism and to implement a custom TenantResolutionContext.  


Implementing a custom HTTPAuthenticationMechanism is one of the intended ways when customizing Quarkus Security
([see docs](https://quarkus.io/guides/security-customization)).  

The extensions build-in TenantResolutionContext expects an ContainerRequestContext, which is not available in the
authentication step. However, to resolve the Tenant via the TenantResolver we only need to extract a specific HTTP-Header,
which we can also get from the RoutingContext.


### Testing
Run `docker compose up -d` to create the databases in the `compose.yaml`, then run `mvn quarkus:dev`.
HTTP Requests for testing are in the `localhost.http`, all passwords are `password`.