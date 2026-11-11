# projector

[![build](https://github.com/farukkavlak/projector/actions/workflows/build.yml/badge.svg)](https://github.com/farukkavlak/projector/actions/workflows/build.yml)

Debloat and control a MUTU HY320 Mini Android projector over Wi-Fi ADB. No root.

| Before | After |
| --- | --- |
| ![Stock home screen](docs/img/home-before.png) | ![Clean home screen](docs/img/home-after.png) |

## Device

| | |
| --- | --- |
| SoC | Allwinner H713 (`sun50iw12p1`) |
| RAM | ~1 GB |
| Screen | 1280x720 |
| Android | 11, not rooted, SELinux permissive |

Findings: [docs/findings.md](docs/findings.md)

Device check: [docs/checklist.md](docs/checklist.md)

## Setup

1. Install [adb](https://developer.android.com/tools/releases/platform-tools).
2. On the projector: enable developer options and network debugging.
3. Connect from the same Wi-Fi network. Accept the prompt on first connect.

## Usage

```sh
export DEV=192.168.1.112:5555   # projector IP:port
export ADB=/path/to/adb         # if not in PATH

scripts/apply.sh --dry-run      # preview
scripts/apply.sh                # debloat (safe to re-run)
scripts/restore.sh              # undo everything
scripts/restore.sh <package>    # undo one package
scripts/snapshot.sh             # save device state (serial/MAC masked)
scripts/lamp.sh off|on|status   # lamp off, device stays online
```

Panel app (phone remote at `http://<projector-ip>:8080`):

```sh
scripts/make-cert.sh                    # local CA + HTTPS cert (once, or after an IP change)
cd panel && ./gradlew assembleDebug
../scripts/sign-panel.sh                 # platform key: live corners, no screen restart
adb install -r app-platform.apk
adb shell appops set local.projector.panel SYSTEM_ALERT_WINDOW allow
```

Build and test without a device:

```sh
scripts/check.sh
```

Open "Panel" once from the launcher. On a phone it has Remote, Picture and System tabs; on a
wider screen it is a two-column layout. It works as a D-pad remote, can type into the focused
field from the phone, lists installed apps with a filter, opens a video link in the matching app
(YouTube, Kick, Prime, TV+, Twitch), uploads local videos and plays them in VLC (500 MB per file,
2 GB total), shows the current video and position, can seek, keeps corner presets
(export/import), and shows device status plus a live screenshot. System also has
brightness/colour, auto keystone/focus switches, a focus nudge, LED switches and a power menu. Needs the Android SDK (`sdk.dir` in `panel/local.properties`).
The sign script downloads the public AOSP platform key once, to `~/.local/android/platform-key`.
An unsigned build still works, but a 4-corner apply restarts the screen for ~25 seconds.

HTTPS runs on `https://<projector-ip>:8443` for the installable phone app. Download the CA from
`http://<projector-ip>:8080/ca.crt`, install it as a CA certificate (Android asks for the screen
lock), then open the HTTPS page and use Chrome's Install app. Sharing a YouTube link to the
installed app opens it in SmartTube. Rerun `scripts/make-cert.sh <ip>` if the IP changes; the CA
stays the same, so the phone does not need to trust a new one.

- Install [FLauncher](https://gitlab.com/flauncher/flauncher) first. Without it, the stock launcher stays.
- Install VLC on the projector for local video playback.
- Wait ~30 s after changes before rebooting, or they are lost.

## Layout

```
config/     packages and settings to change
scripts/    apply, restore, snapshot, lamp, sign-panel, make-cert
snapshots/  device dumps
panel/      Kotlin app: web remote on port 8080
docs/       findings, checklist, images
```
