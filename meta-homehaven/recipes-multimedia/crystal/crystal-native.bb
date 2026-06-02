SUMMARY = "Crystal programming language compiler (native)"
DESCRIPTION = "Pre-built Crystal compiler for the build host. Used at build time \
               only to cross-compile Invidious for aarch64. Never installed in the image."
HOMEPAGE = "https://crystal-lang.org"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"

PV = "1.14.0"

SRC_URI = "https://github.com/crystal-lang/crystal/releases/download/${PV}/crystal-${PV}-1-linux-x86_64.tar.gz"
SRC_URI[sha256sum] = "d39478dbdc978fa1883f4a70f0186ce5054cf3d984e9be99882bdf42a70fe2be"

S = "${WORKDIR}/crystal-${PV}-1"

inherit native

do_configure() {
    :
}

do_compile() {
    :
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/bin/crystal ${D}${bindir}/crystal
    install -m 0755 ${S}/bin/shards  ${D}${bindir}/shards

    # Crystal standard library — compiler needs this at build time
    install -d ${D}${datadir}/crystal
    cp -r ${S}/src ${D}${datadir}/crystal/src

    # LLVM libs bundled with the Crystal release
    install -d ${D}${libdir}
    cp -r ${S}/lib/crystal ${D}${libdir}/crystal
}
