# KoLab Bridge 0.2: dual-APK test

The first APK is KoLab Bridge, the second is a separate UID client application named KoLab Bridge Tester.
Both are READ-ONLY: no root, no kernel module loading.

Steps:
1. Install the two APKs. If old 0.1.0 will not update because of a debug signature mismatch, uninstall old KoLab first.
2. Open KoLab Bridge Tester and press query: expected permission rejection.
3. In KoLab Bridge open Authorized Apps, choose KoLab Bridge Tester (app.kolab.clienttest), and approve its certificate fingerprint.
4. Return to Tester and query: should return JSON containing BRIDGE_READY_READ_ONLY and loadModuleSupported=false.
5. Choose hello.ko in Tester: check that a SHA-256 and compatibility report are returned, with loaded=false. No module is executed.
6. Choose paradise-694-loader_sfx.sh: it must not execute.
7. Revoke Tester authorization in KoLab; subsequent queries should be rejected.

This verifies real Binder across Android app processes, not successful loading via the Gloomy mechanism.
