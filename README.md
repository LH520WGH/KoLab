# KoLab V1

Offline Android kernel-module file manager and **read-only** ELF/.modinfo inspector.

This V1 codebase never executes .ko files, requests Root, calls insmod/rmmod, or reaches any network endpoint. It is a separate experimental app, package `app.kolab.manager`.

Source files are bootstrapped into this private repository via GitHub Actions. After the bootstrap commit, browse `app/src/` for the Kotlin implementation.

See `.github/workflows/build-apk.yml` to build a debug APK from a phone using GitHub Actions.
