#!/bin/sh
# Initialises the Pi-hole gravity database on first boot.
# Subsequent boots skip immediately — the check costs one stat(2) call.
#
# gravity.db must live on /data (writable) rather than in /etc/pihole (read-only rootfs).
# The blocklist source is Steven Black's unified hosts file (~170k domains).

set -e

PIHOLE_DIR="/data/pihole"
GRAVITY_DB="${PIHOLE_DIR}/gravity.db"
BLOCKLIST_URL="https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
ADLIST_COMMENT="StevenBlack unified hosts (added by homehaven-gravity-init)"

mkdir -p "$PIHOLE_DIR"

if [ -f "$GRAVITY_DB" ]; then
    echo "pihole-gravity-init: gravity.db already exists, skipping"
    exit 0
fi

echo "pihole-gravity-init: creating gravity database at ${GRAVITY_DB}"

# Create the gravity.db schema expected by pihole-FTL v5.
# The 'info' version must match what pihole-FTL expects; v5.25 uses version 17.
sqlite3 "$GRAVITY_DB" <<'SQLEOF'
PRAGMA journal_mode=WAL;

CREATE TABLE IF NOT EXISTS "group" (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    enabled     INTEGER NOT NULL DEFAULT 1,
    name        TEXT    NOT NULL UNIQUE,
    date_added  INTEGER NOT NULL DEFAULT (cast(strftime('%s','now') AS INT)),
    date_modified INTEGER NOT NULL DEFAULT (cast(strftime('%s','now') AS INT)),
    description TEXT
);
INSERT INTO "group" (id, name, description) VALUES (0, 'Default', 'The default group');

CREATE TABLE IF NOT EXISTS adlist (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    address         TEXT    NOT NULL UNIQUE,
    enabled         INTEGER NOT NULL DEFAULT 1,
    date_added      INTEGER NOT NULL DEFAULT (cast(strftime('%s','now') AS INT)),
    date_modified   INTEGER NOT NULL DEFAULT (cast(strftime('%s','now') AS INT)),
    comment         TEXT,
    date_updated    INTEGER,
    number          INTEGER NOT NULL DEFAULT 0,
    invalid_domains INTEGER NOT NULL DEFAULT 0,
    status          INTEGER NOT NULL DEFAULT 0,
    abp_entries     INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS adlist_by_group (
    adlist_id INTEGER NOT NULL REFERENCES adlist (id),
    group_id  INTEGER NOT NULL REFERENCES "group" (id),
    PRIMARY KEY (adlist_id, group_id)
);

CREATE TABLE IF NOT EXISTS domainlist (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    type          INTEGER NOT NULL DEFAULT 0,
    domain        TEXT    NOT NULL,
    enabled       INTEGER NOT NULL DEFAULT 1,
    date_added    INTEGER NOT NULL DEFAULT (cast(strftime('%s','now') AS INT)),
    date_modified INTEGER NOT NULL DEFAULT (cast(strftime('%s','now') AS INT)),
    comment       TEXT,
    UNIQUE(domain, type)
);

CREATE TABLE IF NOT EXISTS domainlist_by_group (
    domainlist_id INTEGER NOT NULL REFERENCES domainlist (id),
    group_id      INTEGER NOT NULL REFERENCES "group" (id),
    PRIMARY KEY (domainlist_id, group_id)
);

CREATE TABLE IF NOT EXISTS gravity (
    domain    TEXT    NOT NULL,
    adlist_id INTEGER NOT NULL REFERENCES adlist (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS gravity_domain_idx ON gravity (domain, adlist_id);

CREATE TABLE IF NOT EXISTS info (
    property TEXT PRIMARY KEY,
    value    TEXT NOT NULL
);
INSERT INTO info VALUES ('version', '17');
INSERT INTO info VALUES ('gravity_count', '0');
SQLEOF

echo "pihole-gravity-init: downloading blocklist from ${BLOCKLIST_URL}"

HOSTS_TMP="/tmp/pihole_hosts.$$"
if ! curl -fsSL --retry 3 --retry-delay 5 -o "$HOSTS_TMP" "$BLOCKLIST_URL"; then
    echo "pihole-gravity-init: download failed — pihole will start without blocking"
    rm -f "$HOSTS_TMP"
    exit 0
fi

echo "pihole-gravity-init: importing domains into gravity.db"

# Register the adlist and link it to the default group.
sqlite3 "$GRAVITY_DB" \
    "INSERT INTO adlist (address, comment) VALUES ('${BLOCKLIST_URL}', '${ADLIST_COMMENT}');"
ADLIST_ID=$(sqlite3 "$GRAVITY_DB" \
    "SELECT id FROM adlist WHERE address='${BLOCKLIST_URL}';")
sqlite3 "$GRAVITY_DB" \
    "INSERT INTO adlist_by_group (adlist_id, group_id) VALUES (${ADLIST_ID}, 0);"

# Parse hosts lines (0.0.0.0 <domain>) and bulk-insert in one transaction.
{
    printf 'BEGIN TRANSACTION;\n'
    grep '^0\.0\.0\.0 ' "$HOSTS_TMP" \
        | awk -v id="$ADLIST_ID" '
            $2 != "0.0.0.0" && $2 !~ /^#/ {
                printf "INSERT OR IGNORE INTO gravity(domain,adlist_id) VALUES(\047%s\047,%s);\n", $2, id
            }'
    printf 'COMMIT;\n'
} | sqlite3 "$GRAVITY_DB"

DOMAIN_COUNT=$(sqlite3 "$GRAVITY_DB" "SELECT COUNT(*) FROM gravity;")
sqlite3 "$GRAVITY_DB" \
    "UPDATE adlist SET status=1, number=${DOMAIN_COUNT}, date_updated=cast(strftime('%s','now') AS INT) WHERE id=${ADLIST_ID};"
sqlite3 "$GRAVITY_DB" \
    "UPDATE info SET value='${DOMAIN_COUNT}' WHERE property='gravity_count';"

rm -f "$HOSTS_TMP"
echo "pihole-gravity-init: imported ${DOMAIN_COUNT} domains — done"
