# Android release signing

Package: `ir.asteam.telegrambotstore`

Release alias: `telegrambotstore-release`

SHA-256 certificate fingerprint:

`01:9E:A1:CC:37:F2:3B:26:3C:AA:4F:5A:D5:08:F0:54:4D:E9:DB:CD:F9:C9:58:AF:3C:72:D0:E7:50:AF:4D:69`

## Important

The private JKS keystore and its passwords are intentionally **not committed** to this public repository. Every future production APK must be signed with the same private key so Android can install it as an update over previous versions.

The CI workflow builds an unsigned release APK plus the Android `apksigner` tool. Signing is performed outside the public repository using the private key backup.
