# Allow root password login for development/testing.
# Change to "prohibit-password" (key-only) before any production use.
FILESEXTRAPATHS:prepend := "${THISDIR}/openssh:"

SRC_URI += "file://10-homehaven.conf"

do_install:append() {
    install -d ${D}${sysconfdir}/ssh/sshd_config.d
    install -m 0600 ${UNPACKDIR}/10-homehaven.conf ${D}${sysconfdir}/ssh/sshd_config.d/
}
