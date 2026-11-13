# Device check

Run after installing a new Panel build. Stop at the first failure.

## Install

```sh
scripts/sign-panel.sh
adb install -r panel/app-platform.apk
adb shell appops set local.projector.panel SYSTEM_ALERT_WINDOW allow
```

For a sideloaded app bundle:

```sh
DEV=192.168.1.112:5555 scripts/install-apkm.sh ~/Downloads/com.kick.mobile_1.9.0-*.apkm
```

Open "Panel" once from the launcher and note the IP.

## Base

- `http://<ip>:8080` loads on the phone; `https://<ip>:8443` also loads.
- The lamp pill matches the projector; tap it to turn the lamp off and on.
- D-pad, back/home/menu, volume, play/pause and mute work.
- The screenshot on System refreshes.

## UI

- 390, 768 and 1280px wide: no horizontal scroll.
- The fixed bottom bar does not cover the last block when scrolled to the end.
- Tabs switch Control, Library, Display and System; the active state is clear.
- On desktop the sidebar switches the same views.
- Buttons and inputs are at least 44px tall where they are primary targets.

## Screen view

- The picture on Control updates about once a second.
- Tap a button in an app; if nothing happens (FLauncher ignored it), switch to D-pad and tap the
  matching edge. D-pad mode is the reliable path on TV UIs.
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
- Paste a Kick link: it opens Kick. Kick is leanback-only; tap through the Apps list.
- Paste a TV+ link: it opens TV+.
- Repeat for Prime if it is installed.

## Kick

- Open Kick; a live stream should start from the home carousel.
- Search: navigate to Categories, then left twice and OK. The custom keyboard is D-pad only;
  injected text does not reach it.
- A `kick.com` link opens the app's main screen, not the channel.

## TV+

- Select a profile; the home screen loads.
- Open a series or film and start playback. Video should start; a screenshot during playback may
  be empty because the player is a secure surface.
- Go back and stop playback.

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
