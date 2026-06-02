# HomeHaven 🏠

> Your private, self-hosted home server — flash it, plug it in, and it just works.

---

## What is HomeHaven?

HomeHaven is a minimal, optimized, custom Linux image for Raspberry Pi built entirely with **Yocto Project**. The goal is simple: a single image you flash to an SD card, insert into your Raspberry Pi, and immediately have a fully functional private home server running — no cloud, no subscriptions, no unnecessary bloat.

Everything runs locally. Everything belongs to you.

---

## Why not just use Raspbian?

Raspbian is a great general-purpose OS. But general-purpose means it ships with hundreds of packages, services, and daemons you will never use on a dedicated home server. That's fine for tinkering — not ideal for something running 24/7 on your network.

HomeHaven takes a different approach:

- **Minimal by design** — only what's needed is included, nothing else
- **Smaller attack surface** — fewer packages means fewer potential vulnerabilities
- **Faster boot** — purpose-built images boot significantly faster than general-purpose OS
- **Lower resource usage** — more RAM and CPU left for actual workloads
- **Reproducible** — the entire image is built from source, fully transparent and auditable
- **Stable** — no package manager means nothing accidentally updates and breaks your setup
- **Recoverable** — something goes wrong? Flash a known good image and you're back in minutes

---

## Planned Features

The goal is a single image that covers the core needs of a private home server:

- 📁 **File storage & sync** — your own private cloud (Nextcloud)
- 🎬 **Media server** — stream your own content to any device (Jellyfin)
- 🔒 **Network-wide ad blocking** — cleaner internet on every device at home (Pi-hole)
- 🔑 **Password manager** — self-hosted, your passwords never leave your house (Vaultwarden)
- 🏠 **Home automation** — local control, no cloud dependency (Home Assistant)
- 🌐 **VPN access** — securely reach your home network from anywhere
- 📺 **YouTube without ads** — self-hosted Invidious frontend, no tracking, no ads on any device
- 📊 **Dashboard** — clean overview of your server health and running services

All services are baked into the Yocto image as proper recipes — not installed manually after boot, not running in Docker on top of a generic OS. Purpose built, from the ground up.

---

## Philosophy

Most home server projects get you running fast but leave you dependent on someone else's decisions — their OS, their update schedule, their defaults. HomeHaven is built on the belief that if something runs on your network 24/7, you should understand and control exactly what it is.

That means building from source, documenting every decision, and keeping things as simple as the use case allows.

---

## Getting Started

See **[GETTING_STARTED.md](GETTING_STARTED.md)** for a complete guide — from cloning the repo to a bootable SD card image.

---

## Contributing

The project is in early stages but feedback, ideas, and contributions are welcome. Open an issue if you have suggestions or questions.

---

## License

GPL v2 — same license as the Linux kernel. See [LICENSE](LICENSE).

---

*Built with patience, Yocto, and a Raspberry Pi.*
