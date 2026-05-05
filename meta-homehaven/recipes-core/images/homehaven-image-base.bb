SUMMARY = "HomeHaven base image"
DESCRIPTION = "Minimal bootable foundation for HomeHaven. Boots with systemd, \
               exposes SSH, and mounts the data partition. All HomeHaven service \
               images build on top of this."
LICENSE = "MIT"

inherit core-image extrausers

# ── Users ──────────────────────────────────────────────────────────────────────
# Root password for development/testing. Remove before production.
EXTRA_USERS_PARAMS = "usermod -p '\$6\$pvx3.SblXpg1hmu0\$9XVI242iUum2WmLe9h/fIsxn0ILs.sUj/CQvUqxf.4BTQOm4bYYRitJokD0cn6is5hGdrloJUGokpRfTMm1wY0' root;"

# ── Image features ─────────────────────────────────────────────────────────────
IMAGE_FEATURES += " \
    ssh-server-openssh \
    read-only-rootfs \
"

# ── Packages ───────────────────────────────────────────────────────────────────
IMAGE_INSTALL = " \
    packagegroup-core-boot \
    packagegroup-core-ssh-openssh \
    kernel-modules \
    e2fsprogs \
    util-linux \
    systemd-networkd \
    homehaven-network \
    homehaven-init \
    pihole \
    ${CORE_IMAGE_EXTRA_INSTALL} \
"

# ── Partition layout ───────────────────────────────────────────────────────────
WKS_FILE = "homehaven-rpi4.wks"
IMAGE_FSTYPES = "wic.bz2 wic.bmap ext4"

# ── /data mount point ──────────────────────────────────────────────────────────
# The data partition (LABEL=data) is mounted here at runtime via fstab.
# The directory must exist in the rootfs even though it's empty in the image.
dirs755 = "/data"

# ── read-only rootfs compatibility ─────────────────────────────────────────────
# systemd-compat-units postinstall runs systemctl mask, which writes to /etc.
# That conflicts with read-only-rootfs. A bbappend in recipes-core/systemd/
# overrides the postinstall to a no-op since HomeHaven has no SysV scripts.
BAD_RECOMMENDATIONS += "systemd-compat-units"
PACKAGE_EXCLUDE += "systemd-compat-units"
