# Dev PKI for the mTLS sample

Both distributions serve **HTTPS with optional client certificates**
(`quarkus.http.ssl.client-auth=request`). Browsers and other user-facing
clients connect without a certificate; the machine-to-machine endpoints
*require* one, enforced per endpoint (a path permission restricted to the
mTLS mechanism, plus `@Authenticated`) — no certificate means 401. The
certificate's **subject** (e.g. `CN=oc-admin`) is the machine
identity the peer authenticates — Quarkus maps it to the request's
`SecurityIdentity` automatically.

## Two trust domains, as in production

| component | admin (8443)                    | worker (8444)               |
|-----------|---------------------------------|-----------------------------|
| user endpoints (browser, no cert) | `/hello` (delegates to worker) | —             |
| machine endpoints (client cert required) | —                    | `/capitalize`      |

The admin is browser-facing, so it has **two** certificates:

- `admin-web` — its **browser-facing server certificate**, signed by
  `web-ca`, a stand-in for a *public* CA (in production: e.g. Let's
  Encrypt for the real domain). Browsers validate this chain and never
  see the mTLS certificates.
- `admin-mtls` — its **machine identity** (`CN=oc-admin`), signed by the
  *internal* CA `mtls-ca`. This is the client certificate the admin
  presents when it calls the worker.

The worker is machine-only, so it needs **no public certificate at all**:
`worker` (signed by `mtls-ca`) is its server certificate, and `mtls-ca`
is how it verifies the callers' machine certificates.

Why the two certificates on the admin are separate (and not one cert for
both roles, as this sample's first version did): in production the web
cert's identity is your public domain (90-day rotation, most-exposed
key), while the mTLS cert is an internal machine credential with its own
lifecycle, rotation and revocation — reusing one for both would couple
the machine-to-machine trust to the web presence.

## Generate the certificates (dev only)

```bash
./generate.sh
```

Creates, in this directory:

| file                      | purpose                                                   |
|---------------------------|-----------------------------------------------------------|
| `web-ca.crt` / `web-ca.key`     | dev stand-in for a public CA (browser trust anchor)     |
| `mtls-ca.crt` / `mtls-ca.key`   | internal machine CA (mTLS trust anchor)                 |
| `admin-web.crt` / `admin-web.key`   | admin's browser-facing server cert (`web-ca`, SAN `localhost`) |
| `admin-mtls.crt` / `admin-mtls.key` | admin's machine identity `CN=oc-admin` (client role)   |
| `worker.crt` / `worker.key`       | worker's server cert `CN=oc-worker` (machine-only)      |

Generated files are git-ignored. These certificates are **dev only**
(unencrypted keys, self-signed CAs) — in production the web cert comes
from a public CA, the machine certs from an internal CA (or a service
mesh does this whole layer for you).

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
# user endpoint in the browser (or curl): no client certificate needed,
# server cert validated against the (dev) public CA:
curl --cacert certs/web-ca.crt 'https://localhost:8443/hello?name=opencast'
# → Hello, Opencast!  (admin delegated the capitalization to the worker,
#   presenting its *machine* certificate admin-mtls for that call)

# machine endpoint WITHOUT a client certificate -> 401
curl -i --cacert certs/mtls-ca.crt 'https://localhost:8444/capitalize?text=hello'

# machine endpoint WITH the admin's *machine* certificate -> 200
curl --cacert certs/mtls-ca.crt --cert certs/admin-mtls.crt --key certs/admin-mtls.key \
  'https://localhost:8444/capitalize?text=hello'
# → Hello
```

`--cacert` makes the client trust the server; `--cert`/`--key` present the
client's identity (the mTLS part). Note the two different CAs: the user
endpoint is validated against `web-ca`, the machine endpoint against
`mtls-ca`.

## Using a browser

The browser needs the (dev) public CA trusted for the user endpoints; a
client certificate is only needed for the machine endpoints. Easiest in
Firefox:

1. `Settings → Privacy & Security → Certificates → View Certificates`
2. **Your Certificates** tab → *Import*: select `certs/admin-mtls.crt` and `certs/admin-mtls.key`
3. **Cert Authorities** tab → *Import*: select `certs/web-ca.crt`, check "Trust this certificate to identify websites"

(Chrome on macOS needs a PKCS12 first:
`openssl pkcs12 -export -out admin-mtls.p12 -inkey admin-mtls.key -in admin-mtls.crt -name oc-admin -password pass:oc`,
then import the P12 via *Settings → Security → Manage certificates* and the
CA into the Keychain marked "Always Trust".)
