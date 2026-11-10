# Findings

All verified on the device, 2026-11-10.

## Security

- SELinux is permissive. Shell can `setprop` many `persist.*` values.
- ADB over network is on by default (port 5555). Connect still needs approval on screen.
- Security patch: 2022-02-05. Bootloader locked, `oem_unlock_supported=1`.
- Fingerprint claims to be Google ADT-3.
- `com.htc.*` apps are by Hotack, not HTC.
- Stock launcher contains an ad SDK (`com.zeasn.ad.connector`).
- `com.android.toofifi` runs as `com.huawei.connection`, with system uid, location and phone-state access.
- `com.htc.eventuploadservice` starts on boot and uploads events.

## Keystone

- Corners: `persist.display.keystone_{lb,lt,rt,rb}{x,y}`. Integers, divide by 1000.
- `setprop` works. SurfaceFlinger applies the values on boot (`mKeystoneTran` in logcat).
- Live apply is blocked for shell:
  - SurfaceFlinger transact `1050`: `Operation not permitted`
  - Broadcast `android.intent.hotack_keystone`: protected
- Original values: [snapshots/2026-11-10/before/keystone.txt](../snapshots/2026-11-10/before/keystone.txt)
- Settings screen: `am start -n com.htc.htcsettings/com.htc.activity.ProjectionActivity3`

![Projection settings](img/projection-settings.png)

## Lamp and fan

Use `scripts/lamp.sh`. Device stays online while the lamp is off.

- Lamp: `/sys/devices/platform/soc@2900000/5240000.ge2d/backlight/tv/bl_power`, mode `rw-rw-rw-`.
  - `5` = off, `6` = on. `0` does not turn it on.
- Lamp off also stops the fan.
- Fan guard (`PhoneWindowManager`, `services.jar`):
  - Every 1.8 s it reads `/sys/devices/platform/fan_ctrl/debug_fantrl`.
  - Slow fan + `sys.start_fan_fun=1` + `sys.shutdown_backlight!=1` → counter +1.
  - Counter > 3 → `android.htc.fanctrl` → `com.htc.closedialog` shuts the device down.
  - The counter never resets until it fires.
- Safe order:
  - Off: `sys.shutdown_backlight=1`, `sys.start_fan_fun=0`, then `bl_power=5`.
  - On: `bl_power=6`, wait ~12 s, then `sys.start_fan_fun=1`, `sys.shutdown_backlight=0`.
- Other hardware (from `android.os.PrjScreen` in `framework.jar`): focus motor `/sys/devices/platform/motor_ctr/`,
  angle sensor `.../4-001b/sensor_angle`, auto keystone `persist.sys.tpryxcrt`, auto focus `persist.sys.vafocus`.

## Power

- Sleep (`KEYCODE_SLEEP`) drops Wi-Fi. No wake over network.
- Power key opens a 15 s shutdown countdown. Menu key opens OEM settings.

## Panel app

App in `panel/`, runs as a normal (non-system) app.

- Works without extra rights: media keys (`AudioManager.dispatchMediaKeyEvent`), volume.
- Opening another app from a service is blocked on Android 11 ("Background activity start").
  Fix: declare `SYSTEM_ALERT_WINDOW` in the manifest and run
  `appops set local.projector.panel SYSTEM_ALERT_WINDOW allow`. Both are needed.
- SmartTube opens YouTube links with `ACTION_VIEW` + package `org.smarttube.stable`.
- Lamp needs shell rights. Not in the app yet.

## Performance

| Change | Result |
| --- | --- |
| Remove apps | Data used: 1.4 GB → 835 MB |
| Stock launcher → FLauncher | Free RAM: 398 → 480 MB |
| Google services off | No measurable gain |
| 15 more apps off | Used RAM: 689 → 669 MB |

- FLauncher uses ~93 MB.
- App freezer: not supported by the kernel.
- Shell cannot disable a single app component.

## Gotchas

- Wait ~30 s after `pm uninstall` / `disable-user` before rebooting.
- `adb shell` eats stdin inside `while read`. Use `</dev/null`.
- `pm clear me.efesser.flauncher` also resets the default home. Re-run `apply.sh`.
- SmartTube: use only official GitHub builds 30.56+ (signing key leaked in Nov 2025).
