#!/bin/sh
set -eu

: "${PROD_DB_URL:?Configure PROD_DB_URL}"
: "${PROD_DB_USERNAME:?Configure PROD_DB_USERNAME}"
: "${PROD_DB_PASSWORD:?Configure PROD_DB_PASSWORD}"
: "${PROD_CORS_ORIGIN:?Configure PROD_CORS_ORIGIN}"
: "${PROD_JWT_SECRET:?Configure PROD_JWT_SECRET}"

if [ -n "${DB_CA_CERT_FILE:-}" ]; then
    test -r "$DB_CA_CERT_FILE" || { printf '%s\n' 'No se puede leer el certificado CA de MySQL.' >&2; exit 1; }
    cert_directory=$(mktemp -d)
    keytool -importcert -noprompt -alias mysql-ca -file "$DB_CA_CERT_FILE" \
        -keystore "$cert_directory/truststore.p12" -storetype PKCS12 -storepass changeit >/dev/null 2>&1
    set -- "-Djavax.net.ssl.trustStore=$cert_directory/truststore.p12" \
        '-Djavax.net.ssl.trustStoreType=PKCS12' '-Djavax.net.ssl.trustStorePassword=changeit' "$@"
fi

exec java "$@" -jar /app/backend.jar --spring.config.location=file:/app/application.properties
