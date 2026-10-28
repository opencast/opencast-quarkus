# Dev PKI for the mTLS sample

Both distributions serve **HTTPS with optional client certificates**
(`quarkus.http.ssl.client-auth=request`). Browsers and other user-facing
clients connect without a certificate; the machine-to-machine endpoints
*require* one, enforced per endpoint with `@Authenticated` (no certificate
→ 401). The certificate's **subject** (e.g. `CN=oc-admin`) is the machine
identity the peer authenticates — Quarkus maps it to the request's
`SecurityIdentity` automatically.

Each distribution uses **one certificate for both roles** (serving and
calling): the subject is what the peer sees.

| component | admin (8443)                    | worker (8444)               |
|-----------|---------------------------------|-----------------------------|
| user endpoints (browser, no cert) | `/hello` (delegates to worker) | —             |
| machine endpoints (client cert required) | —                    | `/capitalize`      |

## Generate the certificates (dev only)

```bash
./generate.sh
```

Creates, in this directory:

| file           | purpose                                                  |
|----------------|----------------------------------------------------------|
| `ca.crt` / `ca.key` | dev CA (trust anchor)                             |
| `admin.crt` / `admin.key`   | admin's cert, identity `CN=oc-admin` (server + client) |
| `worker.crt` / `worker.key` | worker's cert, identity `CN=oc-worker` (server + client) |

Generated files are git-ignored. These certificates are **dev only**
(unencrypted keys, self-signed CA) — in production they come from Kubernetes
secrets/volumes, or a service mesh does this whole layer for you.

## Running

The distributions resolve certificates from `../../certs` by default
(module-directory-relative; the dev-mode JVM always runs from the module
directory, no matter where Maven is invoked); override with `CERTS_DIR` if
you generate them elsewhere:

```bash
# from the repo root, in two terminals:

# terminal 1: admin (serves https://localhost:8443)
mvn -P admin quarkus:dev

# terminal 2: worker (serves https://localhost:8444)
mvn -P worker quarkus:dev

# The admin/worker profiles add the respective distribution to the reactor.
# quarkus:dev is silently skipped for the plain jar modules (they have no
# quarkus build goal configured; the skip warning is disabled in
# modules/parent/pom.xml), so only the distribution actually starts.

# Both dev-mode debuggers default to port 5005. If you start them one after
# the other, the second one detects the conflict and moves to a random free
# port ("Changed debug port to ... because of a port conflict" in its log).
# If you want a stable port to attach your IDE to, pin it explicitly:
#   mvn ... -Ddebug-port=5006
```

Try it:

```bash
# user endpoint in the browser (or curl): no client certificate needed.
# (The browser must trust the CA first; see below.)
curl --cacert certs/ca.crt 'https://localhost:8443/hello?name=opencast'
# → Hello, Opencast!  (admin delegated the capitalization to the worker)

# machine endpoint WITHOUT a client certificate -> 401
curl -i --cacert certs/ca.crt 'https://localhost:8444/capitalize?text=hello'

# machine endpoint WITH the admin's client certificate -> 200
curl --cacert certs/ca.crt --cert certs/admin.crt --key certs/admin.key \
  'https://localhost:8444/capitalize?text=hello'
# → Hello
```

`--cacert` makes the client trust the server; `--cert`/`--key` present the
client's identity (the mTLS part).

## Using a browser

The browser needs the CA trusted *and*, for the machine endpoints, a client
certificate. Easiest in Firefox:

1. `Settings → Privacy & Security → Certificates → View Certificates`
2. **Your Certificates** tab → *Import*: select `certs/admin.crt` and `certs/admin.key`
3. **Cert Authorities** tab → *Import*: select `certs/ca.crt`, check "Trust this certificate to identify websites"

(Chrome on macOS needs a PKCS12 first:
`openssl pkcs12 -export -out admin.p12 -inkey admin.key -in admin.crt -name oc-admin -password pass:oc`,
then import the P12 via *Settings → Security → Manage certificates* and the CA
into the Keychain marked "Always Trust".)
