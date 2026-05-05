# Getting Started

This guide takes you from a fresh Linux machine to a bootable HomeHaven SD card image.

---

## Before you begin

| Requirement | Detail |
|---|---|
| **Host OS** | Ubuntu 22.04 LTS (other Debian-based distros may work) |
| **Disk space** | ~100 GB free (`build/tmp/` alone grows to 50–60 GB) |
| **RAM** | 8 GB minimum, 16 GB recommended |
| **Time** | First build: 4–8 hours. Subsequent builds: minutes (sstate cache) |
| **SD card** | 8 GB minimum for flashing |

---

## 1. Install host dependencies

```bash
sudo apt-get update
sudo apt-get install -y \
    gawk wget git diffstat unzip texinfo gcc build-essential \
    chrpath socat cpio python3 python3-pip python3-pexpect \
    xz-utils debianutils iputils-ping python3-git python3-jinja2 \
    python3-subunit libegl1-mesa libsdl1.2-dev mesa-common-dev \
    zstd liblz4-tool file locales libacl1
```

---

## 2. Clone the repositories

HomeHaven is built from several Yocto layers cloned side by side. All must be on the **walnascar** branch.

```bash
mkdir HomeHaven && cd HomeHaven

# Yocto base system
git clone --depth=1 -b walnascar https://git.yoctoproject.org/poky

# Extended package layers
git clone --depth=1 -b walnascar https://git.openembedded.org/meta-openembedded

# Raspberry Pi BSP
git clone --depth=1 -b walnascar https://git.yoctoproject.org/meta-raspberrypi

# HomeHaven custom layer (this repo)
git clone https://github.com/YOUR_USERNAME/HomeHaven meta-homehaven
```

Your directory should look like this:

```
HomeHaven/
  poky/
  meta-openembedded/
  meta-raspberrypi/
  meta-homehaven/
```

---

## 3. Initialize the build environment

Run this every time you open a new terminal:

```bash
source poky/oe-init-build-env build
```

This creates the `build/` directory and drops you inside it. BitBake commands only work after sourcing this script.

---

## 4. Configure the build

`oe-init-build-env` creates default config files in `build/conf/`. Replace their contents with the HomeHaven configuration below.

### `build/conf/bblayers.conf`

Replace `/path/to/HomeHaven` with the **absolute path** to your `HomeHaven/` directory.

```bitbake
POKY_BBLAYERS_CONF_VERSION = "2"

BBPATH = "${TOPDIR}"
BBFILES ?= ""

BBLAYERS ?= " \
  /path/to/HomeHaven/poky/meta \
  /path/to/HomeHaven/poky/meta-poky \
  /path/to/HomeHaven/poky/meta-yocto-bsp \
  /path/to/HomeHaven/meta-openembedded/meta-oe \
  /path/to/HomeHaven/meta-openembedded/meta-python \
  /path/to/HomeHaven/meta-openembedded/meta-networking \
  /path/to/HomeHaven/meta-raspberrypi \
  /path/to/HomeHaven/meta-homehaven \
  "
```

### `build/conf/local.conf`

Replace the generated file entirely:

```bitbake
MACHINE ??= "raspberrypi4-64"
DISTRO ?= "homehaven"

# Development access — remove before production use
EXTRA_IMAGE_FEATURES ?= "allow-empty-password empty-root-password allow-root-login"

USER_CLASSES ?= "buildstats"
PATCHRESOLVE = "noop"
CONF_VERSION = "2"

# Shallow git clones — avoids multi-GB kernel source downloads
BB_GIT_SHALLOW = "1"
BB_GENERATE_SHALLOW_TARBALLS = "1"

# Parallelism — adjust BB_NUMBER_THREADS and PARALLEL_MAKE to your CPU count
BB_NUMBER_THREADS = "8"
PARALLEL_MAKE = "-j 8"
PARALLEL_MAKE:pn-gcc = "-j 1"

# curl 8.12.x has a broken ptest
PTEST_ENABLED:pn-curl = "0"

BB_DISKMON_DIRS ??= "\
    STOPTASKS,${TMPDIR},1G,100K \
    STOPTASKS,${DL_DIR},1G,100K \
    STOPTASKS,${SSTATE_DIR},1G,100K \
    STOPTASKS,/tmp,100M,100K \
    HALT,${TMPDIR},100M,1K \
    HALT,${DL_DIR},100M,1K \
    HALT,${SSTATE_DIR},100M,1K \
    HALT,/tmp,10M,1K"
```

---

## 5. Verify the layer setup

```bash
bitbake-layers show-layers
```

You should see all 8 layers listed with no errors.

---

## 6. Build the image

```bash
bitbake homehaven-image-base
```

The first build downloads and compiles everything from source — expect several hours. Go make coffee.

Output lands in:

```
build/tmp/deploy/images/raspberrypi4-64/
  homehaven-image-base-raspberrypi4-64.rootfs.wic.bz2   # flashable image
  homehaven-image-base-raspberrypi4-64.rootfs.wic.bmap  # block map for fast flashing
```

---

## 7. Flash to SD card

**Using bmaptool (recommended — much faster than dd):**

```bash
sudo apt-get install -y bmap-tools

sudo bmaptool copy \
  build/tmp/deploy/images/raspberrypi4-64/homehaven-image-base-raspberrypi4-64.rootfs.wic.bz2 \
  /dev/sdX   # replace sdX with your SD card device
```

**Using dd (slower fallback):**

```bash
bzcat build/tmp/deploy/images/raspberrypi4-64/homehaven-image-base-raspberrypi4-64.rootfs.wic.bz2 \
  | sudo dd of=/dev/sdX bs=4M status=progress conv=fsync
```

> **Warning:** Double-check your device path with `lsblk` before writing. Writing to the wrong device will destroy data.

---

## 8. First boot

Insert the SD card into your Raspberry Pi 4 and power it on.

**SSH access (development only):**

```bash
ssh root@<pi-ip-address>
# password: homehaven
```

Find the Pi's IP address from your router's DHCP client list, or connect a monitor on first boot to see it printed at login.

> **Note:** SSH is included in this image for development and debugging only. It will not be present in production images.

**What happens on first boot:**

1. The data partition (`/data`) is expanded to fill the SD card
2. Pi-hole downloads its blocklist and populates the gravity database (~1–2 minutes, requires internet)
3. The Pi-hole DNS server starts

After first boot completes, point your router's DHCP DNS setting to the Pi's IP address to enable network-wide ad blocking.

---

## Rebuilding after changes

```bash
# Required at the start of every terminal session
source poky/oe-init-build-env build

# Rebuild only what changed
bitbake homehaven-image-base

# Force a specific recipe to rebuild from scratch
bitbake -c cleansstate <recipe-name>
bitbake homehaven-image-base
```

---

## Troubleshooting

**SSH connection refused:**
Give the Pi 30–60 seconds to finish first-boot services. Pi-hole gravity init can take 1–2 minutes on a slow connection.

**Pi-hole not blocking ads:**
Check that your router or device is configured to use the Pi's IP as its DNS server. Pi-hole only blocks traffic routed through it.

---

