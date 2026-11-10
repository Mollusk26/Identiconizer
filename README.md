## 2026 update (Android 16)

This fork brings Identiconizer! up to date:

* Builds with Gradle 8 / AGP 8.13, AndroidX, compileSdk/targetSdk 36 (Android 16), minSdk 26.
* New **Pixel creatures** style: mirrored pixel-art critters generated from each contact's name.
* Automatic identicons for new contacts now use a JobScheduler content trigger instead of an
  always-running service (which Android 8+ kills).
* Fixes for Android 12-16: PendingIntent mutability flags, foreground-service types,
  notification channels and permission, exported components, edge-to-edge insets.
* The Xposed module was removed. HoloColorPicker is vendored, since JCenter is gone.

APKs are built by GitHub Actions (Actions tab → latest run → `Identiconizer-apk` artifact).

---

Identiconizer!
==============
This is a port of ChameleonOS' contact identicons feature (available in the
JellyBean versions) with some additional features and fixes.
When enabled, new contacts will be assigned a unique identicon instead of the
default picture.

XDA Thread
==========
http://forum.xda-developers.com/showthread.php?t=2718943
