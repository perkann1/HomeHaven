SUMMARY = "HomeHaven network configuration"
DESCRIPTION = "DHCP on all ethernet interfaces via systemd-networkd, \
               and a systemd preset that enables networkd at boot."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://eth.network \
    file://90-homehaven.preset \
"

S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"

RDEPENDS:${PN} = "systemd-networkd"

FILES:${PN} = " \
    ${systemd_unitdir}/network/eth.network \
    ${systemd_unitdir}/system-preset/90-homehaven.preset \
"

do_install() {
    install -d ${D}${systemd_unitdir}/network
    install -m 0644 ${UNPACKDIR}/eth.network ${D}${systemd_unitdir}/network/

    install -d ${D}${systemd_unitdir}/system-preset
    install -m 0644 ${UNPACKDIR}/90-homehaven.preset ${D}${systemd_unitdir}/system-preset/
}
