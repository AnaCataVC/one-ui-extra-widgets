# Retro Music Widgets — Architecture & Visual Specs

[English](retro-music-widgets.md) | [Español](retro-music-widgets.es.md)

The Retro Music suite provides three home-screen players built with **Jetpack Glance** (`SizeMode.Exact`), custom `Canvas` bitmaps (`WidgetCanvasRenderer`), and a shared `MediaSessionManager` + `AudioManager` bridge (`MediaNotificationListener`).

---

## 1. iPod Classic (`IpodClassicWidget`)
- **Provider:** `app/src/main/res/xml/widget_music_ipod_info.xml` (`2×2` default, `resizeMode="horizontal|vertical"`)
- **Responsive Modes (`calculateIpodLayout`):**
  - `COMPACT`: Standard `2×2` vertical stack with scaled Click Wheel.
  - `TALL`: Expands the LCD display height (`34%` of inner height, up to 2 title lines) and centers the scaled Click Wheel in the lower body using weighted spacers.
  - `WIDE`: Switches to a horizontal `Row` with the LCD on the left and the Click Wheel on the right.
- **Click Wheel Controls:** 5 proportional touch zones (`MENU` opening `MainActivity`, `⏮` previous, center/bottom `▶‖` play/pause, `⏭` next), rendered at `360px` via `WidgetCanvasRenderer.renderIpodClickWheelBitmap`.
- **LCD Readout:** Emoji-free status header (`iPod • Now Playing` / `iPod • One UI`) and dynamic device model fallback from `Build.MANUFACTURER` / `Build.MODEL` when no media session is active.

## 2. 2000s USB MP3 Stick (`RetroMp3Widget`)
- **Provider:** `app/src/main/res/xml/widget_music_mp3_info.xml` (`4×1` default, `resizeMode="horizontal|vertical"`)
- **Visual Design:** Indigo-cobalt One UI stick shell (`#3F46A8` day / `#2D3380` night) with `26.dp` One UI corners, right-side USB cap seam with lanyard slot notch, ergonomic circular controls on the left (`⏮`, larger primary `▶`/`⏸`, `⏭`), and a high-contrast dark display bezel (`#0E121B`) with gold header labels (`DIGITAL MP3`, `USB`), two-line visual hierarchy (cyan track title + soft slate artist/app subtitle), 5-bar equalizer graphic, and bottom cyan progress bar.
- **Responsive Modes (`calculateMp3Layout`):**
  - `PILL`: Horizontal stick layout; compacts controls and hides the USB cap below `240.dp` width (and equalizer below `220.dp`) to preserve readout legibility.
  - `STACKED`: Stacks the dark display bezel on top and redistributes the circular controls across the bottom when stretched vertically.

## 3. Circular Portable CD Discman (`DiscmanWidget`)
- **Provider:** `app/src/main/res/xml/widget_music_discman_info.xml` (`2×2` default, `resizeMode="horizontal|vertical"`)
- **Visual Design:** 1:1 circular matte-graphite Discman body (`WidgetCanvasRenderer.renderDiscmanBodyBitmap`, `440px`) with metallic bevel rim, `COMPACT DISC / DIGITAL AUDIO • ESP ANTI-SHOCK` branding, bottom perforated speaker grille arc, and transparent outer corners.
- **Central Pill Controls (`calculateDiscmanLayout`):** Scales `discSizeDp = min(width, height)` to preserve a 1:1 circle at any grid size, with a centered pill module containing `⏮`, `≡` (menu, shown when `discSizeDp >= 190.dp`), **cyan** backlit LCD, `▶`/`⏸`, and `⏭`.

## 4. Media Transport & Widget Picker Previews
- **Transport Fallback (`MediaNotificationListener.sendTransportCommand`):** Uses `MediaController.transportControls` when an active `MediaSession` is available (pausing on both `STATE_PLAYING` and `STATE_BUFFERING`) and falls back to `AudioManager.dispatchMediaKeyEvent` when no controller is active. Refreshes widget state immediately and after `250 ms`.
- **One UI Widget Picker Previews:** Each provider declares `android:previewLayout` (`res/layout/widget_preview_*.xml` + `res/drawable/*preview*.xml`) using `RemoteViews`-compatible views so the One UI Home widget picker renders an accurate preview instead of the app icon.
