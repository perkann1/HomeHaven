SUMMARY = "Invidious — self-hosted YouTube frontend"
DESCRIPTION = "Privacy-respecting alternative to YouTube. No ads, no tracking. \
               Runs locally; all data lives on /data/invidious/."
HOMEPAGE = "https://invidious.io"
LICENSE = "AGPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=127c562a116841d4d3c4c4208034bede"

# ── Build-time: Crystal compiler + target C libraries ─────────────────────────
DEPENDS = "crystal-native-native openssl pcre2 sqlite3 zlib gmp libxml2 libevent gc"

# ── Sources ───────────────────────────────────────────────────────────────────
# Main source + all shards from shard.lock (SQLite variant — pg is excluded).
# destsuffix places each shard where Crystal expects it: ${WORKDIR}/shards/<name>
SRC_URI = " \
    git://github.com/iv-org/invidious.git;protocol=https;nobranch=1;name=invidious \
    git://github.com/crystal-lang/crystal-db.git;protocol=https;nobranch=1;name=db;destsuffix=shards/db \
    git://github.com/kemalcr/kemal.git;protocol=https;nobranch=1;name=kemal;destsuffix=shards/kemal \
    git://github.com/iv-org/protodec.git;protocol=https;nobranch=1;name=protodec;destsuffix=shards/protodec \
    git://github.com/athena-framework/negotiation.git;protocol=https;nobranch=1;name=negotiation;destsuffix=shards/athena-negotiation \
    git://github.com/mamantoha/http_proxy.git;protocol=https;nobranch=1;name=http_proxy;destsuffix=shards/http_proxy \
    git://github.com/luislavena/radix.git;protocol=https;nobranch=1;name=radix;destsuffix=shards/radix \
    git://github.com/crystal-lang/crystal-sqlite3.git;protocol=https;nobranch=1;name=sqlite3;destsuffix=shards/sqlite3 \
    git://github.com/sija/backtracer.cr.git;protocol=https;nobranch=1;name=backtracer;destsuffix=shards/backtracer \
    git://github.com/crystal-loot/exception_page.git;protocol=https;nobranch=1;name=exception_page;destsuffix=shards/exception_page \
    file://config.yml \
    file://invidious.service \
"

SRCREV_invidious    = "0e0ee40cb6e5c88679666544dc8f19cb956b028b"
SRCREV_db           = "3eaac85a5d4b7bee565b55dcb584e84e29fc5567"
SRCREV_kemal        = "75d5ef10465f1d42e232d0454ff926cf004e3d4f"
SRCREV_protodec     = "9e02d88a19f7b948877f0650297dad4949188e52"
SRCREV_negotiation  = "5fc45d1908ef3fc6428cdef2178d70832dc90379"
SRCREV_http_proxy   = "ea17189f3bd05a2a89a18abdf73cae534b57faa6"
SRCREV_radix        = "e2e402fcaf2d06f124c9bd2801dfb169293ecb71"
SRCREV_sqlite3      = "c58cea290c85e2a33dc8f494a5f04b519d3e0274"
SRCREV_backtracer   = "07d6dc43817d16c7fb7f0ab51d7d67325a469534"
SRCREV_exception_page = "a5261c2e7d087f8f02dab39a3a7d0c7de93d12a1"

SRCREV_FORMAT = "invidious"
PV = "2.20260207.0"

S = "${WORKDIR}/git"
UNPACKDIR = "${WORKDIR}/sources"

inherit systemd

SYSTEMD_SERVICE:${PN} = "invidious.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

RDEPENDS:${PN} = "openssl sqlite3"

do_configure:prepend() {
    # Wire up vendored shards into the lib/ directory Crystal expects.
    # Mirrors what `shards install` would do — each shard in lib/<shard-name>/.
    mkdir -p ${S}/lib
    for shard in db kemal protodec http_proxy radix sqlite3 backtracer exception_page; do
        rm -rf ${S}/lib/${shard}
        cp -r ${WORKDIR}/shards/${shard} ${S}/lib/${shard}
    done
    rm -rf ${S}/lib/athena-negotiation
    cp -r ${WORKDIR}/shards/athena-negotiation ${S}/lib/athena-negotiation
}

do_compile() {
    # Crystal standard library from the native sysroot
    export CRYSTAL_PATH="${RECIPE_SYSROOT_NATIVE}${datadir}/crystal/src"

    # Target libraries for the linker
    export PKG_CONFIG_LIBDIR="${STAGING_DIR_TARGET}${libdir}/pkgconfig:${STAGING_DIR_TARGET}${datadir}/pkgconfig"
    export PKG_CONFIG_SYSROOT_DIR="${STAGING_DIR_TARGET}"

    cd ${S}

    # Step 1: Crystal compiles to an aarch64 object file via LLVM.
    # --cross-compile emits invidious.o — no executable yet.
    crystal build \
        --cross-compile \
        --target aarch64-unknown-linux-gnu \
        --release \
        -D use_sqlite3 \
        src/invidious.cr \
        -o ${B}/invidious

    # Step 2: Link the object file with aarch64 libraries using the Yocto cross-linker.
    ${CC} ${LDFLAGS} ${B}/invidious.o -o ${B}/invidious \
        -L${STAGING_DIR_TARGET}${libdir} \
        -lpthread -lssl -lcrypto -lpcre2-8 -lsqlite3 -lz -lgmp -lxml2 -levent -lm -ldl -lgc
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/invidious ${D}${bindir}/invidious

    install -d ${D}${sysconfdir}/invidious
    install -m 0644 ${UNPACKDIR}/config.yml ${D}${sysconfdir}/invidious/config.yml

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/invidious.service ${D}${systemd_system_unitdir}/invidious.service
}

FILES:${PN} = " \
    ${bindir}/invidious \
    ${sysconfdir}/invidious/ \
    ${systemd_system_unitdir}/invidious.service \
"
