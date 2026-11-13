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

Screen control: [docs/screen-control.md](docs/screen-control.md)

UI notes: [docs/ui.md](docs/ui.md)

## Security

The Panel has no login. Keep the projector on a trusted LAN and turn off network debugging
when not in use. The device trusts the public AOSP platform key, so a build signed with it
gets signature permissions; see [docs/findings.md](docs/findings.md).

## Setup

1. Install [adb](https://developer.android.com/tools/releases/platform-tools).
2. On the projector: enable developer options and network debugging.
3. Connect from the same Wi-Fi network. Accept the prompt on first connect.

## Usage

```sh
export DEV=192.168.1.50:5555   # projector IP:port
export ADB=/path/to/adb         # if not in PATH

scripts/apply.sh --dry-run      # preview
scripts/apply.sh                # debloat (safe to re-run)
scripts/restore.sh              # undo everything
scripts/restore.sh <package>    # undo one package
scripts/snapshot.sh             # save device state (serial/MAC masked)
scripts/lamp.sh off|on|status   # lamp off, device stays online
scripts/install-apkm.sh <file.apkm>  # install an APKMirror bundle
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

Open "Panel" once from the launcher. On a phone it has Control, Library, Display and System tabs; on
a wider screen it is a sidebar with the same sections. Control has a live screen view: tap or drag
the picture to control the projector, or switch to D-pad mode if the app ignores touch. It also
works as a D-pad remote, types into the focused field from the phone, and has playback, volume and
sleep controls. Library lists installed apps with a filter, opens a video link in the matching app
(YouTube, Kick, Prime, TV+, Twitch), and uploads local videos to play in VLC (500 MB per file, 2 GB
total). Display keeps corner presets and the picture/projector settings. System shows device status,
a live screenshot, power and PWA setup. Needs the Android SDK (`sdk.dir` in
`panel/local.properties`).
The sign script downloads the public AOSP platform key once, to `~/.local/android/platform-key`.
An unsigned build still works, but a 4-corner apply restarts the screen for ~25 seconds.

HTTPS runs on `https://<projector-ip>:8443` for the installable phone app. Download the CA from
`http://<projector-ip>:8080/ca.crt`, install it as a CA certificate (Android asks for the screen
lock), then open the HTTPS page and use Chrome's Install app. Sharing a link to the installed
app opens it in the matching app. Rerun `scripts/make-cert.sh <ip>` if the IP changes; the CA
stays the same, so the phone does not need to trust a new one.

- Install [FLauncher](https://gitlab.com/flauncher/flauncher) first. Without it, the stock launcher stays.
- Install VLC on the projector for local video playback.
- Wait ~30 s after changes before rebooting, or they are lost.

## Layout

```
config/     packages and settings to change
scripts/    apply, restore, snapshot, lamp, sign-panel, make-cert, install-apkm
snapshots/  device dumps
panel/      Kotlin app: web remote on port 8080
docs/       findings, checklist, images
```
