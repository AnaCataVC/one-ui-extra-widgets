# Retro Music Widgets — Architecture & Visual Specs

The Retro Music suite provides six home-screen players built with **Jetpack Glance** (`SizeMode.Exact`), custom `Canvas` bitmaps (`MusicCanvasRenderer` via `WidgetCanvasRenderer`), and a shared `MediaSessionManager` + `AudioManager` bridge (`MediaNotificationListener`).

---

## 1. iPod Classic (`IpodClassicWidget`)
- **Provider:** `app/src/main/res/xml/widget_music_ipod_info.xml` (`2×2` default, `resizeMode="horizontal|vertical"`)
- **Responsive Modes (`calculateIpodLayout`):**
  - `COMPACT`: Standard `2×2` vertical stack with scaled Click Wheel.
  - `TALL`: Expands the LCD display height (`34%` of inner height, up to 2 title lines) and centers the scaled Click Wheel in the lower body using weighted spacers.
  - `WIDE`: Switches to a horizontal `Row` with the LCD on the left and the Click Wheel on the right.
- **Click Wheel Controls:** 5 proportional touch zones (`MENU` opening `MainActivity`, `⏮` previous, center/bottom `▶`/`⏸` play/pause, `⏭` next), rendered at `360px` via `WidgetCanvasRenderer.renderIpodClickWheelBitmap` with a dynamic Play/Pause icon inside the center button.
- **LCD Readout:** Rounded album artwork (or music-note placeholder), track title, dynamic device model fallback from `Build.MANUFACTURER` / `Build.MODEL`, favorite heart indicator, One UI progress bar, and elapsed/duration timestamps (`0:00`).

## 2. 2000s USB MP3 Stick (`RetroMp3Widget`)
- **Provider:** `app/src/main/res/xml/widget_music_mp3_info.xml` (`4×1` default, `resizeMode="horizontal|vertical"`)
- **Visual Design:** Indigo-cobalt One UI stick shell (`#3F46A8` day / `#2D3380` night) with `26.dp` One UI corners, right-side USB cap seam with lanyard slot notch, ergonomic circular controls on the left (`⏮`, larger primary `▶`/`⏸`, `⏭`), and a high-contrast dark display bezel (`#0E121B`) with gold header labels (`DIGITAL MP3`, `USB`), two-line visual hierarchy (cyan track title + soft slate artist/app subtitle), 5-bar equalizer graphic, and bottom cyan progress bar.
- **Responsive Modes (`calculateMp3Layout`):**
  - `PILL`: Horizontal stick layout; compacts controls and hides the USB cap below `240.dp` width (and equalizer below `220.dp`) to preserve readout legibility.
  - `STACKED`: Stacks the dark display bezel on top and redistributes the circular controls across the bottom when stretched vertically.

## 3. Circular Portable CD Discman (`DiscmanWidget`)
- **Provider:** `app/src/main/res/xml/widget_music_discman_info.xml` (`2×2` default, `resizeMode="horizontal|vertical"`)
- **Visual Design:** 1:1 circular matte-graphite Discman body (`WidgetCanvasRenderer.renderDiscmanBodyBitmap`, `440px`) with metallic bevel rim, top-right lavender lid-release latch accent, `COMPACT DISC / DIGITAL AUDIO • ESP ANTI-SHOCK` branding, bottom perforated speaker grille arc, and transparent outer corners.
- **LCD & Ergonomic Controls (`calculateDiscmanLayout`):** Scales `discSizeDp = min(width, height)` to preserve a 1:1 circle at any grid size, with an upper-middle **cyan** backlit LCD (`CD • PLAY`/`PAUSE`, battery indicator, track title, `00:00` timestamp, and subtitle without album art) above a 4-button row (`⏮`, larger primary `▶`/`⏸`, `⏭`, and `≡` menu).

## 4. 80s/90s Cassette Walkman (`CassetteWalkmanWidget`)
- **Provider:** `app/src/main/res/xml/widget_music_cassette_info.xml` (`2×2` default, `resizeMode="horizontal|vertical"`, `previewLayout="@layout/widget_preview_cassette"`)
- **Responsive Modes (`calculateCassetteLayout`):** Adapts across `COMPACT`, `TALL`, and `WIDE` (`12:7` cassette aspect ratio).
- **Canvas Deck (`renderCassetteDeckBitmap`):** Gunmetal Walkman shell (`26.dp` One UI corners) housing a smoky acrylic cassette window, Type II CrO2 label header, corner micro-screws, 3-digit tape counter (`000`–`360`), and dual 6-tooth tape reels whose wound magnetic tape radii interpolate smoothly with track progress (`positionMs / durationMs`) above 4 beveled physical transport keys (`⏮`, `▶`/`⏸`, `⏭`, `≡`).

## 5. Minimalist Vinyl Turntable (`VinylTurntableWidget`)
- **Provider:** `app/src/main/res/xml/widget_music_vinyl_info.xml` (`2×2` default, `resizeMode="horizontal|vertical"`, `previewLayout="@layout/widget_preview_vinyl"`)
- **Responsive Modes (`calculateVinylLayout`):** Adapts across `COMPACT`, `TALL`, and `WIDE` (`COMPACT` / `TALL` stack platter, readout, and controls; `WIDE` places platter left and readout + controls right).
- **Canvas Platter & Tonearm (`renderVinylTurntableBitmap`):** Dieter Rams / Braun-inspired satin plinth (`26.dp` corners) with brushed aluminum platter, stroboscopic speed dots, micro-grooved black LP with anisotropic light sheen, circular-clipped album artwork label (or classic orange/cream studio label), and an articulated S-shaped tonearm tracking playback progress above `33⅓ RPM • HI-FI` readout and 4 circular controls.

## 6. 2000s Hi-MD MiniDisc Player (`MiniDiscWidget`)
- **Provider:** `app/src/main/res/xml/widget_music_minidisc_info.xml` (`4×1` default, `resizeMode="horizontal|vertical"`, `previewLayout="@layout/widget_preview_minidisc"`)
- **Responsive Modes (`calculateMiniDiscLayout`):** Horizontal `PILL` stick (`4×1`) and vertical `STACKED` layout when stretched vertically (`heightDp >= 120dp && widthDp < heightDp * 2.2`).
- **Canvas Cartridge & EL Display (`renderMiniDiscBadgeBitmap`):** Slate-blue metallic Hi-MD body with translucent cartridge badge revealing an iridescent magneto-optical disc & silver shutter, paired with a dark electroluminescent bezel (`HI-MD • ATRAC DSP`, cyan-lime track title, progress bar, spectrum bars) and ergonomic silver jog-dial transport buttons.

## 7. Media Transport & Widget Picker Previews
- **Transport Fallback (`MediaNotificationListener.sendTransportCommand`):** Uses `MediaController.transportControls` when an active `MediaSession` is available (pausing on both `STATE_PLAYING` and `STATE_BUFFERING`) and falls back to `AudioManager.dispatchMediaKeyEvent` when no controller is active. Refreshes widget state immediately and after `300 ms`.
- **One UI Widget Picker Previews:** Each provider declares `android:previewLayout` (`res/layout/widget_preview_*.xml` + `res/drawable/*preview*.xml`) using `@RemoteView`-compatible views (`LinearLayout`, `FrameLayout`, `ImageView`, `TextView`) so the One UI Home widget picker renders an accurate Day/Night preview instead of the app icon.
