SUMMARY = "HomeHaven first-boot initialization"
DESCRIPTION = "Expands the data partition to fill the SD card on first boot, \
               then mounts it at /data on every boot."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://homehaven-expand-data \
    file://homehaven-expand-data.service \
    file://data.mount \
"

S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"

inherit systemd

RDEPENDS:${PN} = "e2fsprogs util-linux"

SYSTEMD_SERVICE:${PN} = " \
    homehaven-expand-data.service \
    data.mount \
"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

FILES:${PN} = " \
    /usr/sbin/homehaven-expand-data \
    ${systemd_system_unitdir}/homehaven-expand-data.service \
    ${systemd_system_unitdir}/data.mount \
"

do_install() {
    install -d ${D}/usr/sbin
    install -m 0755 ${UNPACKDIR}/homehaven-expand-data ${D}/usr/sbin/

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/homehaven-expand-data.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${UNPACKDIR}/data.mount ${D}${systemd_system_unitdir}/
}
