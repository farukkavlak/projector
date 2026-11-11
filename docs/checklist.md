# Device check

Run after installing a new Panel build. Stop at the first failure.

## Install

```sh
scripts/sign-panel.sh
adb install -r panel/app-platform.apk
adb shell appops set local.projector.panel SYSTEM_ALERT_WINDOW allow
```

Open "Panel" once from the launcher and note the IP.

## Base

- `http://<ip>:8080` loads on the phone; `https://<ip>:8443` also loads.
- The lamp pill matches the projector; tap it to turn the lamp off and on.
- D-pad, back/home/menu, volume, play/pause and mute work.
- The screenshot on System refreshes.

## Screen view

- The picture on Remote updates about once a second.
- Tap a button in an app; if nothing happens, switch to D-pad and tap the matching edge.
- Drag a list and check that it scrolls.
- Tap a text field, then type from the phone.

## Search from the phone

- Open Kick or YouTube and focus a search box.
- Panel -> Type on projector -> type a query -> Send: the text appears on the projector.
- Search sends the text and Enter in one tap.
- Backspace and Clear work. Turkish characters worked in SmartTube search.

## Apps and links

- The Apps list loads; the filter narrows it; tapping an app opens it.
- Paste a YouTube link: it opens in SmartTube.
- Paste a Kick link: it opens in the Kick app, or at least its main screen.
- Repeat for Prime and TV+ if those apps are installed.

## Local video

- Upload a small MP4: progress reaches 100% and the file appears.
- Play opens VLC; seeking works.
- Delete removes the file and the usage line drops.
- A file over 500 MB is rejected before the upload starts.

## Keystone and lamp

- Live corners apply without a screen restart; presets save, apply and delete.
- Tilt and size sliders change the picture.
- Lamp off keeps the device online; lamp on re-arms the fan guard.

## After a reboot

- Panel starts on boot.
- Keystone corners survive the reboot.
- The PWA is still installed and the share target still works.

## Known risks

- TV+ may refuse a non-approved device (DRM/attestation).
- The Kick TV package name may differ; find it in the app list.
- Local video needs VLC on the projector.
