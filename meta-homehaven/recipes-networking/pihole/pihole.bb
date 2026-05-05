SUMMARY = "Pi-hole ad-blocking DNS server — configuration and services"
DESCRIPTION = "Config files, systemd service, and first-boot gravity initialisation \
               for Pi-hole FTL. All mutable data lives under /data/pihole/."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://pihole-FTL.conf \
    file://setupVars.conf \
    file://01-pihole.conf \
    file://pihole.service \
    file://pihole-gravity-init.service \
    file://pihole-gravity-init.sh \
    file://resolv.conf \
"

S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"

inherit systemd

# sqlite3 CLI needed by gravity init script; curl for blocklist download.
RDEPENDS:${PN} = "pihole-ftl sqlite3 curl"

SYSTEMD_SERVICE:${PN} = " \
    pihole.service \
    pihole-gravity-init.service \
"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    # Pi-hole config (read-only in rootfs — all mutable paths redirect to /data).
    install -d ${D}${sysconfdir}/pihole
    install -m 0644 ${UNPACKDIR}/pihole-FTL.conf  ${D}${sysconfdir}/pihole/
    install -m 0644 ${UNPACKDIR}/setupVars.conf    ${D}${sysconfdir}/pihole/

    install -d ${D}${sysconfdir}/dnsmasq.d
    install -m 0644 ${UNPACKDIR}/01-pihole.conf    ${D}${sysconfdir}/dnsmasq.d/

    # Static DNS for the Pi itself.
    install -m 0644 ${UNPACKDIR}/resolv.conf       ${D}${sysconfdir}/resolv.conf

    # Systemd units.
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/pihole.service              ${D}${systemd_system_unitdir}/
    install -m 0644 ${UNPACKDIR}/pihole-gravity-init.service ${D}${systemd_system_unitdir}/

    # Gravity init script.
    install -d ${D}${sbindir}
    install -m 0755 ${UNPACKDIR}/pihole-gravity-init.sh ${D}${sbindir}/pihole-gravity-init
}

FILES:${PN} = " \
    ${sysconfdir}/pihole/ \
    ${sysconfdir}/dnsmasq.d/01-pihole.conf \
    ${sysconfdir}/resolv.conf \
    ${systemd_system_unitdir}/pihole.service \
    ${systemd_system_unitdir}/pihole-gravity-init.service \
    ${sbindir}/pihole-gravity-init \
"
