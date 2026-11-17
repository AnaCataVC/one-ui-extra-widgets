# One UI Astrology: Transits & Retrogrades (`OneUiAstrologyWidget`)

The **One UI Astrology: Transits & Retrogrades** widget (`OneUiAstrologyWidget`) is a 100% offline Jetpack Glance home-screen widget designed for **Samsung One UI 6.1 – 8.5**. It computes real-time tropical zodiac positions and planetary retrograde states locally via `Astronomy Engine` (`io.github.cosinekitty:astronomy`).

> **Separation of Concerns:** Lunar phase illumination, synodic age, and elongation live exclusively in the scientific `OneUiMoonPhaseWidget` ([Weather & Lunar Widgets](weather-widgets.md)). `OneUiAstrologyWidget` focuses strictly on astrological transits and planetary motion.

---

## Visual Layout & Responsive Sizing (`SizeMode.Exact`)

`OneUiAstrologyWidget` uses `SizeMode.Exact` and delegates dimension math to `calculateAstrologyLayout(widthDp, heightDp)` across three responsive tiers:

1. **Hero Header (`Canvas` Orbital Emblem + Mercury Status):**
   - Renders a deterministic `Bitmap` orbital badge via `WidgetCanvasRenderer.renderAstrologyEmblemBitmap` featuring a vector Mercury glyph (`☿`) and status-colored sweep arc (Blue for Direct, Purple for Pre/Post-Retrograde Shadow, Amber for Retrograde).
   - Displays Mercury's live motion state, current zodiac sign, and a summary of any planets currently in retrograde.
2. **Sun Sign & Daily Moon Transit Cards (`Canvas` Zodiac Badges):**
   - Renders bespoke, emoji-free circular zodiac badges via `WidgetCanvasRenderer.renderZodiacSignBitmap` with element-tinted backdrops (Fire, Earth, Air, Water) and custom vector line-art for all 12 signs.
   - **Sun Sign:** Current tropical Sun sign and ecliptic degree within the sign (`0°–30°`, shown in `STANDARD` and `EXPANDED` modes).
   - **Moon Transit:** Current tropical zodiac sign of the Moon today.
3. **5-Planet Transit Strip (`STANDARD` & `EXPANDED`):**
   - Displays **Mercury, Venus, Mars, Jupiter, and Saturn** with mini `Canvas` zodiac badges, 3-letter planet codes (`Mer`, `Ven`, `Mar`, `Jup`/`Júp`, `Sat`), and color-coded motion indicators (`R` for Retrograde, `S` for Shadow in `EXPANDED` mode).

| Layout Mode | Trigger Bounds | Emblem Size | Sun/Moon Degrees | 5-Planet Strip | Motion Tags (`R`/`S`) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `COMPACT` | `width < 220dp` or `height < 120dp` | `34dp` | Hidden | Hidden | Hidden |
| `STANDARD` (4x2 default) | `width >= 220dp` and `120dp <= height < 195dp` | `42dp` | Shown | Shown (`height >= 145dp`) | Color-coded |
| `EXPANDED` (4x3+) | `width >= 220dp` and `height >= 195dp` | `48dp` | Shown | Shown | Shown (`R` / `S`) |

---

## Widget Picker Preview (`previewLayout`)

- **Provider:** `app/src/main/res/xml/widget_astrology_info.xml` (`4×2` default, `resizeMode="horizontal|vertical"`)
- **Preview Layout:** `app/src/main/res/layout/widget_preview_astrology.xml`
- **Vector Drawable:** `app/src/main/res/drawable/ic_preview_astrology.xml`

---

## Companion Screen Interaction

Tapping anywhere on `OneUiAstrologyWidget` launches `MainActivity` directly on the **Astrology** tab (`AstrologyDetailScreen`), which presents full planetary degrees, daily angular speed ($\Delta^\circ/\text{day}$), and Direct/Retrograde/Shadow badges.
