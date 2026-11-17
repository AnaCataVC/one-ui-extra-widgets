# One UI 5G & Wi-Fi Signal Quality Widget (`OneUiNetworkWidget`)

`OneUiNetworkWidget` is a `SizeMode.Exact` Jetpack Glance home-screen widget designed around Samsung One UI (`26.dp` outer corner radius, `12.dp` concentric inner cards, and adaptive Day/Night surfaces) that monitors real-time **5G NR / 4G LTE** cellular signal strength and **Wi-Fi 6** link quality without polling loops.

---

## 1. Responsive Layout Modes (`calculateNetworkLayout`)

The widget defaults to a **`2×2` square** (`targetCellWidth="2"`, `targetCellHeight="2"`, `resizeMode="horizontal|vertical"`) in `app/src/main/res/xml/widget_network_info.xml` and redistributes its hierarchy across three layout modes via `calculateNetworkLayout(widthDp, heightDp)`:

- **`SQUARE` (`2×2` Default, `heightDp >= 125dp && widthDp < 240dp`):**
  - Top One UI header (`Signal • 5G & Wi-Fi`) with a `26.dp` circular refresh hit-zone (`↻`).
  - Two vertically stacked `InnerCardSurface` cards:
    - **Cellular / 5G Card:** `Canvas` stepped signal badge (`renderCellularSignalBitmap`), active generation (`5G NR` / `4G LTE`), semantic quality label, `dBm` readout, and carrier name.
    - **Wi-Fi Card:** `Canvas` concentric wave badge (`renderWifiSignalBitmap`), `SSID`, semantic quality label, link speed (`Mbps`), frequency band (`6 GHz` / `5 GHz` / `2.4 GHz`), and `RSSI` (`dBm`).
- **`COMPACT_PILL` (`2×1` / `4×1`, `heightDp < 125dp`):**
  - Omits the header bar to maximize vertical density and places the Cellular and Wi-Fi `InnerCardSurface` pills side by side with scaled `24.dp–36.dp` `Canvas` signal badges and a trailing refresh button.
- **`EXPANDED` (`4×2`, `heightDp >= 125dp && widthDp >= 240dp`):**
  - Full One UI header plus two side-by-side `InnerCardSurface` dashboards with `38.dp–52.dp` `Canvas` signal badges, large primary readouts (`17.sp`), and full carrier/band metadata.

---

## 2. Visual Enhancements & Semantic Color Tokens

Instead of plain Unicode bar characters, `WidgetCanvasRenderer` renders deterministic, anti-aliased `Bitmap` badges (`128×128 px`) with soft tinted circular backdrops and semantic signal colors:

| Signal Level | Bars | Quality Label (EN / ES) | Day Accent | Night Accent |
| :--- | :---: | :--- | :---: | :---: |
| **Excellent** | `4 / 4` | Excellent / Excelente | `#059669` | `#34D399` |
| **Good** | `3 / 4` | Good / Buena | `#0381FE` | `#3E9BFF` |
| **Fair** | `2 / 4` | Fair / Regular | `#D97706` | `#FBBF24` |
| **Weak** | `1 / 4` | Weak / Débil | `#DC2626` | `#F87171` |
| **Disconnected** | `0 / 4` | No Signal / Sin señal | `#94A3B8` | `#64748B` |

---

## 3. One UI Widget Picker Preview (`previewLayout`)

- **Provider XML:** `app/src/main/res/xml/widget_network_info.xml` (`android:previewLayout="@layout/widget_preview_network"`)
- **Preview Layout:** `app/src/main/res/layout/widget_preview_network.xml` mirrors the default `2×2` stacked layout using `RemoteViews`-compatible `LinearLayout`, `ImageView`, `TextView`, and `Space` views.
- **Vector Icons:** `ic_preview_signal_5g.xml` and `ic_preview_signal_wifi.xml` reference adaptive `@color/oneui_preview_*` tokens in `res/values/colors.xml` and `res/values-night/colors.xml`.

---

## 4. Data Source & Permissions

- **Monitor:** `NetworkQualityMonitor.readSnapshot(context)` reads `TelephonyManager`, `CellSignalStrengthNr`, `CellSignalStrengthLte`, `ConnectivityManager`, and `WifiManager`.
- **Permissions:** Uses `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `READ_PHONE_STATE` (for `dataNetworkType` 5G NR / LTE detection), and `ACCESS_FINE_LOCATION` / `NEARBY_WIFI_DEVICES` (for Wi-Fi SSID). Gracefully falls back when optional permissions are not granted.
