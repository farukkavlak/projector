# UI

Small design system for the Panel. Keep changes inside it.

## Palette

| Token | Value | Use |
| --- | --- | --- |
| `--night` | `#0e0f11` | page background, inputs, editor |
| `--surface` | `#17181b` | cards |
| `--surface-2` | `#212327` | buttons inside cards |
| `--line` | `#2a2c31` | borders, dividers, slider track |
| `--text` | `#f4f1ea` | primary text |
| `--muted` | `#9a9da5` | labels and secondary text |
| `--beam` | `#e8a33d` | accent: primary action, on state, active tab |
| `--lamp` | `#f0b35b` | lamp status, warnings |
| `--bad` | `#e06c5f` | destructive actions and errors |

One accent only. No gradients, glassmorphism or decorative shadows.

## Structure

- A panel is a stack of blocks. A block is a section label (`h2`) and one card.
- Cards have no border. Buttons inside cards are filled (`--surface-2`), also borderless.
- Inputs are recessed: `--night` fill with a `--line` border.
- Settings are `.card.list` with 48px rows and dividers.
- Help text is one short line at most. Prefer a clear label over a note.

## Type

- Body: 14px / 1.45, system UI stack. Buttons 13px, small buttons and notes 12px.
- Section label: 12px, 600, muted, no uppercase.
- Numbers use `font-variant-numeric: tabular-nums`.

## Spacing

- Scale: 4 / 8 / 12 / 16 / 20 / 24.
- Page gutter: 16px mobile, 24px desktop.
- Label to card 8px, block to block 20px, card padding 12px, items inside a card 12px.

## Sizes

- Button 40px high, radius 8. Small button (`.small`) 32px.
- D-pad keys 48px, transport 44px, play 56px.
- Cards radius 12.

## Layout

- Mobile: fixed 56px bottom bar with four tabs: Control, Library, Display, System.
- 600-899px: same, content capped at 640px and centred.
- Desktop (>= 900px): 180px sidebar, no bottom bar.
- >= 1000px: each panel is two columns (`.cols` with two `.col`); on smaller screens the
  columns flow into one.
- Only the active panel is shown.

## Checks

- 390 / 768 / 1280px: no horizontal scroll.
- The bottom bar does not cover the last block.
- Active tab is visible on the bottom bar and the sidebar.
- Destructive actions use `--bad`; there is no second accent.
