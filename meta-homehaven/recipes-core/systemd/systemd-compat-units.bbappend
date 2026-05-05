# HomeHaven is pure systemd — no SysV init compat needed.
# systemd-compat-units has a pkg_postinst_ontarget script that runs systemctl
# to mask SysV services at first boot. That requires a writable /etc, which
# conflicts with read-only-rootfs. Override to a no-op since we have no SysV
# scripts to mask anyway.
pkg_postinst_ontarget:${PN} () {
    :
}
