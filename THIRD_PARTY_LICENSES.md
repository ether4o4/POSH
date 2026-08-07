# Third-Party Licenses

## Upstream project (Apache-2.0)

POSH is a modified fork of an upstream Kotlin Multiplatform assistant project by Simon Schubert, released under the Apache License 2.0:

- **Source:** https://github.com/SimonSchubert/Kai
- **License:** Apache-2.0
- **Copyright:** Copyright the upstream contributors

As required by Apache-2.0 §4, this notice is retained. POSH modifies and extends the upstream code; the changes are summarized in [`README.md`](README.md). The full Apache-2.0 license text is in [`LICENSE.txt`](LICENSE.txt).

## Bundled binaries

This project includes the following third-party binaries in `androidApp/src/main/jniLibs/`:

## PRoot (Termux fork)

- **Files:** `libproot.so`, `libproot-loader.so`, `libproot-loader32.so`
- **Source:** https://github.com/termux/proot
- **License:** GPL-2.0
- **Copyright:** Copyright (C) PRoot developers

PRoot is a user-space implementation of chroot, mount --bind, and binfmt_misc. It is used to run an Alpine Linux environment inside the Android app without requiring root access. PRoot is executed as a separate process and is not linked into the application code.

The full GPL-2.0 license text is available at: https://www.gnu.org/licenses/old-licenses/gpl-2.0.html

## talloc

- **Files:** `libtalloc.so`
- **Source:** https://talloc.samba.org/
- **License:** LGPL-3.0
- **Copyright:** Copyright (C) Andrew Tridgell, Stefan Metzmacher, and contributors

talloc is a hierarchical memory allocator used as a dependency of PRoot. It is dynamically linked.

The full LGPL-3.0 license text is available at: https://www.gnu.org/licenses/lgpl-3.0.html
