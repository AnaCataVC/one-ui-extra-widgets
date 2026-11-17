# Widgets de Música Retro — Arquitectura y Especificaciones Visuales

[English](retro-music-widgets.md) | [Español](retro-music-widgets.es.md)

La suite de Música Retro ofrece tres reproductores para la pantalla de inicio construidos con **Jetpack Glance** (`SizeMode.Exact`), gráficos `Canvas` (`WidgetCanvasRenderer`) y un puente compartido de `MediaSessionManager` + `AudioManager` (`MediaNotificationListener`).

---

## 1. iPod Classic (`IpodClassicWidget`)
- **Proveedor:** `app/src/main/res/xml/widget_music_ipod_info.xml` (`2×2` por defecto, `resizeMode="horizontal|vertical"`)
- **Modos responsivos (`calculateIpodLayout`):**
  - `COMPACT`: Distribución vertical `2×2` con rueda clic escalada.
  - `TALL`: Amplía la pantalla LCD (`34%` de la altura interna, hasta 2 líneas de título) y centra la rueda clic escalada en el cuerpo inferior mediante espaciadores con peso.
  - `WIDE`: Cambia a fila horizontal (`Row`) con la pantalla LCD a la izquierda y la rueda clic a la derecha.
- **Controles de la rueda clic:** 5 zonas táctiles proporcionales (`MENU` abre `MainActivity`, `⏮` anterior, centro/inferior `▶‖` reproducir/pausar, `⏭` siguiente), renderizada a `360px` mediante `WidgetCanvasRenderer.renderIpodClickWheelBitmap`.
- **Pantalla LCD:** Encabezado limpio sin emojis (`iPod • Reproduciendo` / `iPod • En pausa`) y modelo de dispositivo dinámico leído de `Build.MANUFACTURER` / `Build.MODEL` cuando no hay sesión activa.

## 2. Reproductor MP3 USB 2000s (`RetroMp3Widget`)
- **Proveedor:** `app/src/main/res/xml/widget_music_mp3_info.xml` (`4×1` por defecto, `resizeMode="horizontal|vertical"`)
- **Diseño visual:** Carcasa azul cobalto (`#27359B` día / `#1A236B` noche) con esquinas `26.dp` de One UI, línea de unión de tapa USB a la derecha, botones circulares plateados a la izquierda (`⏮`, botón principal `▶`/`⏸`, `⏭`) y bisel negro brillante (`#0F1117`) con etiquetas doradas (`DIGITAL MP3 PLAYER`, `MP3 / FM`, `USB`) rodeando una pantalla LCD retroiluminada en **cyan** (`#35E8F7` día / `#1FD6E8` noche).
- **Modos responsivos (`calculateMp3Layout`):**
  - `PILL`: Barra horizontal; reduce los botones secundarios a `30.dp` si el ancho baja de `220.dp`.
  - `STACKED`: Apila el bisel negro + LCD cyan arriba y redistribuye los botones plateados a lo ancho en la parte inferior al estirarse verticalmente.

## 3. Discman CD Portátil Circular (`DiscmanWidget`)
- **Proveedor:** `app/src/main/res/xml/widget_music_discman_info.xml` (`2×2` por defecto, `resizeMode="horizontal|vertical"`)
- **Diseño visual:** Cuerpo circular 1:1 en grafito mate (`WidgetCanvasRenderer.renderDiscmanBodyBitmap`, `440px`) con bisel metálico, rotulado `COMPACT DISC / DIGITAL AUDIO • ESP ANTI-SHOCK`, rejilla de altavoz perforada en arco inferior y fondo exterior transparente.
- **Módulo central tipo píldora (`calculateDiscmanLayout`):** Escala `discSizeDp = min(width, height)` para conservar siempre la proporción circular 1:1 en cualquier tamaño de cuadrícula, con módulo central que incluye `⏮`, `≡` (menú, visible cuando `discSizeDp >= 190.dp`), pantalla LCD **cyan**, `▶`/`⏸` y `⏭`.

## 4. Controles multimedia y vistas previas en el selector
- **Fallback de transporte (`MediaNotificationListener.sendTransportCommand`):** Usa `MediaController.transportControls` cuando hay una `MediaSession` activa (pausando tanto en `STATE_PLAYING` como en `STATE_BUFFERING`) y recurre a `AudioManager.dispatchMediaKeyEvent` si no hay controlador activo. Refresca el estado inmediatamente y tras `250 ms`.
- **Vistas previas en el selector de One UI:** Cada proveedor declara `android:previewLayout` (`res/layout/widget_preview_*.xml` + `res/drawable/*preview*.xml`) usando vistas compatibles con `RemoteViews` para mostrar una vista previa fiel del widget en lugar del ícono genérico de la app.
