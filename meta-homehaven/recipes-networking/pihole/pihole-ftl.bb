SUMMARY = "Pi-hole FTL - DNS resolver and ad-blocking engine"
DESCRIPTION = "A heavily modified dnsmasq fork that provides Pi-hole's \
               DNS resolution, blocking, and query logging."
HOMEPAGE = "https://github.com/pi-hole/FTL"
LICENSE = "EUPL-1.2"
LIC_FILES_CHKSUM = "file://LICENSE;md5=ad78970d0f0174fa07b68411168b2378"

# nettle provides libhogweed/libnettle (DNSSEC crypto)
DEPENDS = "libcap nettle libidn"

SRC_URI = " \
    git://github.com/pi-hole/FTL.git;protocol=https;nobranch=1 \
    file://0001-fix-lua_scripts-linker-language-cross-compile.patch \
"
SRCREV = "eb1978910d521332ae95f24ad3acf1f9b3931360"
PV = "5.22"

S = "${WORKDIR}/git"

inherit cmake

EXTRA_OECMAKE = " \
    -DCMAKE_BUILD_TYPE=Release \
"

# GCC 14 introduced stricter warnings that pihole-FTL v5.22 triggers.
# Suppress them without modifying upstream source.
TARGET_CFLAGS:append = " -Wno-calloc-transposed-args -Wno-enum-int-mismatch -Wno-use-after-free"

do_configure:prepend() {
    # Pre-generate inspect.lua.hex using od (xxd not available in sysroot).
    # scripts.h includes this file inside a C array initialiser, so it must
    # contain only comma-separated hex bytes, not a full xxd -i declaration.
    od -v -An -t x1 "${S}/src/lua/scripts/inspect.lua" | \
        awk '{for(i=1;i<=NF;i++) printf "0x%s, ", $i}' \
        > "${S}/src/lua/scripts/inspect.lua.hex"

    # Remove the cmake custom command block that depends on xxd.
    sed -i '/# Compile files from raw.*into hex/,/endforeach()/d' \
        "${S}/src/lua/scripts/CMakeLists.txt"

    # Remove hardcoded -DHAVE_READLINE from SQLITE_DEFINES; readline headers
    # are not available in the cross-compile sysroot.
    sed -i 's/ -DHAVE_READLINE//' "${S}/src/CMakeLists.txt"

    # Guard readline/history.h include in ftl_lua.c — it is unconditional in
    # the source but should only be included when LUA_USE_READLINE is set.
    sed -i 's|#include <readline/history.h>|#if defined(LUA_USE_READLINE)\n#include <readline/history.h>\n#endif|' \
        "${S}/src/lua/ftl_lua.c"
}

do_install() {
    install -d ${D}${sbindir}
    install -m 0755 ${B}/pihole-FTL ${D}${sbindir}/pihole-FTL
}

FILES:${PN} = "${sbindir}/pihole-FTL"
