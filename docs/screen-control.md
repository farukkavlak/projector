# Screen control

The Panel shows the projector screen and sends taps back. The goal is to control apps without
the physical remote.

## Capture

- `Preview.kt` calls the hidden `SurfaceControl.screenshot(Rect, 1280, 720, false, displayId)`
  through reflection. `READ_FRAME_BUFFER` and `ACCESS_SURFACE_FLINGER` come from the platform
  signature.
- The live view polls `/preview.jpg` about once a second. JPEG at quality 65 keeps the frames
  small; `SurfaceControl` is serialized so only one capture runs at a time.
- The screenshot is the unwarped 1280x720 framebuffer. Keystone correction is applied by the
  projector after compositing, so the image does not show the warp. Coordinates still match the
  app, not the projected shape.
- DRM-protected video can be a black or empty surface. The UI and menus still capture.

## Input

- `Touch.kt` builds `MotionEvent`s and calls `Instrumentation.sendPointerSync`. `INJECT_EVENTS`
  comes from the platform signature.
- A tap is `ACTION_DOWN` then `ACTION_UP` at the mapped 1280x720 coordinate. A drag sends
  `ACTION_MOVE` steps and is used as a swipe.
- The web view maps the picture rectangle to 1280x720, so a tap on the phone lands on the same
  logical pixel.
- Many TV apps are D-pad only and ignore touch. The Screen section has a **D-pad** mode: a tap in
  the left, right, top or bottom zone sends that arrow, and the center sends OK.

## Limits

- One to two frames per second, not video. This is for menus, search and login, not watching.
- Touch support varies per app. If a tap does nothing, switch to D-pad mode and use the existing
  navigation keys.
- No audio, no true mouse cursor, no text-field focus by tap guarantee.
- The Panel has no authentication. Keep it on a trusted LAN.

## Alternatives

- `scrcpy` on a computer gives smooth 30-60 fps video and a real mouse, but it needs a computer
  and is not part of the phone Panel.
- `MediaProjection` could capture at a higher rate, but needs a consent dialog, a foreground
  activity and more memory; this device has 1 GB of RAM.
- An `AccessibilityService` could click by node text/ID, but needs the service enabled and adds a
  large amount of code. Not worth it until touch proves insufficient.

## Device check

1. Open an app and confirm the live picture updates.
2. Tap a button; the app should react. If not, switch to D-pad and tap the matching edge.
3. Drag a list and check that it scrolls.
4. Try a text field: tap it, then use Type on projector.
5. Try Kick and TV+ specifically; both are TV UIs and may ignore touch.
