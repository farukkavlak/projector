# UI

Small design system for the Panel. Keep changes inside it.

## Palette

| Token | Value | Use |
| --- | --- | --- |
| `--night` | `#0e0f11` | page background |
| `--surface` | `#17181b` | cards, inputs |
| `--surface-2` | `#1d1f23` | file buttons, secondary blocks |
| `--line` | `#26282d` | borders and dividers |
| `--text` | `#f4f1ea` | primary text |
| `--muted` | `#9a9da5` | labels and secondary text |
| `--beam` | `#e8a33d` | accent, active state, primary action |
| `--lamp` | `#f0b35b` | lamp status |
| `--bad` | `#e06c5f` | destructive and errors |

One accent only. No gradients except the tiny player art placeholder, no glassmorphism, no
decorative shadows.

## Type

- Body: 14px / 1.55, system UI stack.
- Section label: 12px, 600, muted, no uppercase, no letter-spacing.
- Page title: 24px when a view needs one.
- Numeric values use `font-variant-numeric: tabular-nums`.

## Spacing

- 8px grid: 8 / 12 / 16 / 20 / 24 / 32.
- Page gutter: 18px mobile, 28px desktop.
- Section gap: 20px. Card padding: 14-16px.
- Touch targets at least 44px.

## Shape

- Cards and editors: 12px radius.
- Buttons and inputs: 10px.
- Chips and small controls: 7-8px.
- Switches may stay rounded; avoid 999px pills for ordinary actions.

## Layout

- Mobile: fixed bottom bar with four tabs: Control, Library, Display, System.
- Desktop (>= 900px): 200px sidebar + 980px content; no bottom bar.
- Control is a two-column dashboard on desktop: screen + player on the left, transport/navigation/
  typing/sleep on the right.
- Only the active panel is shown on both layouts.

## Checks

- 390 / 768 / 1280px: no horizontal scroll.
- The fixed bottom bar does not cover the last block when scrolled to the end.
- Tab active state is visible on both the bottom bar and the sidebar.
- Contrast is readable, destructive actions use `--bad`, and there is no second accent color.
