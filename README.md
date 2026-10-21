<div align="center">

# 🥽 SmartTube VR

**YouTube sin anuncios en tus Meta Quest, dentro de un cine virtual.**

SmartTube original de siempre, con SponsorBlock y DeArrow, un inicio tipo *launcher* y 16 escenarios alrededor del vídeo. Todo se maneja con el mando, y pronto también con las manos.

[![Compilación](https://github.com/pilahito/SmartTube-VR/actions/workflows/smarttube-source-apk.yml/badge.svg)](https://github.com/pilahito/SmartTube-VR/actions/workflows/smarttube-source-apk.yml)
![Meta Quest](https://img.shields.io/badge/Meta%20Quest-APK%20arm64-1c1e20)
![Android](https://img.shields.io/badge/Android-5.0%2B-3ddc84)
[![Licencia MIT](https://img.shields.io/badge/licencia-MIT-blue)](LICENSE)

![La nueva interfaz de SmartTube VR en Meta Quest 3](docs/quest3/banner.jpg)

| Inicio | Reproductor | Escenarios |
|---|---|---|
| ![Inicio en Quest 3](docs/interfaz/inicio.jpg) | ![Reproductor con SponsorBlock y Deshacer salto](docs/interfaz/reproductor.jpg) | ![Menú de los 16 escenarios](docs/interfaz/escenarios.jpg) |

<sub>*Así se verá la nueva interfaz para Quest 3. Son diseños de muestra, no capturas de la app real.*</sub>

</div>

---

## ✨ Qué trae

### 🏠 Inicio tipo launcher
- Al abrir la app aparece un **inicio con iconos grandes**, dibujado sobre el escenario que hayas elegido.
- **YouTube** abre el SmartTube de siempre.
- **Añadir app** fija las apps que tengas instaladas, con su icono real. Por ejemplo, **Brave** para ver tus series. Si tienes Brave instalado, se fija solo la primera vez.
- **Añadir web** crea accesos directos a tus webs favoritas. Se abren en Brave si lo tienes y, si no, en el navegador predeterminado.
- **Mover y quitar iconos**: mantén pulsado el icono, o pulsa **Y / Menú** en el mando. **Organizar** hace lo mismo desde una lista.

### 📐 Se adapta a la ventana
- El número de columnas, el tamaño de los iconos y los textos cambian con la ventana.
- Funciona al redimensionar el panel de Quest, en **horizontal y en vertical** y con cualquier proporción.
- Navegación completa con el **mando o la cruceta**. No hace falta pantalla táctil.

### 🎬 16 escenarios de cine
| Naturaleza | Salones | Playas | Neón y otros |
|---|---|---|---|
| Bosque natural | Salón | Playa | Retro Arcade |
| Bosque mágico ✨ | Salón nocturno (chimenea) 🔥 | Playa tropical 🌴 | Gamer RGB |
| Montañas | Salón moderno | Playa al atardecer 🌅 | Cyberpunk |
| Espacio | Salón futurista 🚀 | Playa de noche 🌙 | Cine clásico |

En la reproducción, pulsa **Escenarios** (o **Menú / Y** en el mando) para cambiar:
- paisaje, tamaño de pantalla, distancia virtual y brillo;
- el modo **ECO**, que usa menos geometría;
- tu escenario favorito.

### 🛡️ SponsorBlock / AdStop
- **Equilibrado**: salta patrocinios, autopromociones y recordatorios de suscripción, y salta con aviso intros, finales y resúmenes.
- **Intensivo**: además salta intros, resúmenes, finales y relleno.
- Los momentos destacados siempre se respetan.

### 🏷️ DeArrow
Con un clic cambias a títulos y miniaturas limpios, sin *clickbait*, o vuelves a los originales.

<details>
<summary>🖼️ Ver los 16 escenarios actuales (renders)</summary>

![Los 16 escenarios (renders)](docs/escenarios.png)

| | |
|---|---|
| ![Bosque natural](docs/escenarios/00-natural-forest.png) | ![Bosque mágico](docs/escenarios/08-magic-forest.png) |
| ![Salón](docs/escenarios/09-living-room.png) | ![Salón nocturno](docs/escenarios/10-night-lounge-fireplace.png) |
| ![Salón moderno](docs/escenarios/11-modern-lounge.png) | ![Salón futurista](docs/escenarios/12-futuristic-lounge.png) |
| ![Playa](docs/escenarios/07-beach.png) | ![Playa tropical](docs/escenarios/13-tropical-beach.png) |
| ![Playa al atardecer](docs/escenarios/14-sunset-beach.png) | ![Playa de noche](docs/escenarios/15-beach-at-night.png) |
| ![Retro Arcade](docs/escenarios/01-retro-arcade.png) | ![Gamer RGB](docs/escenarios/02-gamer-rgb.png) |
| ![Cyberpunk](docs/escenarios/03-cyberpunk.png) | ![Cine clásico](docs/escenarios/04-classic-cinema.png) |
| ![Espacio](docs/escenarios/05-space.png) | ![Montañas](docs/escenarios/06-mountains.png) |

Se generan con [`tools/previews/RenderPreviews.java`](tools/previews/RenderPreviews.java) a partir de la geometría real, con la pantalla al 50 %.
</details>

---

## 🚀 Lo próximo: versión para Meta Quest 3 y NebulaVR
> 🛠️ **En desarrollo.** Nada de esta sección está todavía en la versión publicada. Las imágenes de arriba muestran cómo se verá.

Sigue siendo **el SmartTube real y completo** (sin anuncios, cuentas, suscripciones, historial y búsqueda), pero pensado para las gafas y no para la tele.

### 🥽 Hecho para las gafas
- **Sin interfaz de tele**: todo se maneja con el láser de los mandos o con las manos.
- **Tamaño de gafas**: textos, botones y tarjetas más grandes, en un panel redimensionable de Horizon OS.
- **Subtítulos más grandes** de serie, legibles dentro de las gafas.
- **4K a 60 fps** de serie en Quest 3, 3S y Pro.

### ▶️ Reproductor mejorado
- **SponsorBlock**: los patrocinios se marcan en **amarillo** en la barra, y cuando se salta un tramo aparece un botón **Deshacer salto** durante 5 segundos.
- **DeArrow** activado de serie: títulos y miniaturas sin *clickbait*.
- **ReturnYouTubeDislike**: vuelve a mostrar los "no me gusta".

### 🖥️ Pantalla virtual a tu gusto
- **Forma**: plana o curva, con la curvatura que quieras.
- **Formato**: 16:9, 21:9, 32:9, 4:3 o Ajustar al vídeo.
- **Tamaño, distancia, altura e inclinación**, con el joystick o pellizcando con las manos.
- **Cómo verlo**: Cine, Tumbado (en el techo), Escritorio, 3D lado a lado o arriba y abajo, 180° y 360°.
- Se guarda un ajuste por modo, y puedes guardar tus favoritos (por ejemplo "Cine grande" o "Monitor de escritorio").

![Panel Pantalla virtual](docs/interfaz/pantalla-ajustes.jpg)

<sub>*La pantalla curva y los modos 3D, 180° y 360° se verán en la pantalla de cine 3D ([SmartTube-VR-3D](https://github.com/pilahito/SmartTube-VR-3D) o NebulaVR). En la ventana 2D de las Quest se ajustan el formato, el tamaño y la distancia.*</sub>

### 💡 Luz ambiente que se adapta al vídeo
La interfaz es la misma, pero la sala se tiñe con los colores y la luz de lo que estás viendo.

| Vídeo de ciudad cyberpunk | Vídeo del fondo del mar |
|---|---|
| ![La sala en magenta y cian](docs/interfaz/adapta-cyberpunk.jpg) | ![La sala en azul con reflejos de agua](docs/interfaz/adapta-oceano.jpg) |

<sub>*Diseños de muestra. En la app, los tramos de SponsorBlock son siempre amarillos.*</sub>

### 🌌 Integrado con NebulaVR
- Se abre desde la tarjeta de SmartTube VR del launcher [NebulaVR](https://github.com/pilahito/NebularVR-Quest).
- Usa el mismo tema de colores que elijas allí: playa de día, noche morada, atardecer o bosque esmeralda.
- El vídeo también se podrá ver en la pantalla de cine 3D de [SmartTube-VR-3D](https://github.com/pilahito/SmartTube-VR-3D).

### 🔄 Sin perder nada
Se firmará con la misma clave que ahora, así que se instalará encima de tu versión actual sin perder tus ajustes.

---

## 📥 Instalar en Meta Quest

**1. Descarga el APK.** En [Releases](https://github.com/pilahito/SmartTube-VR/releases), baja el archivo que termina en **`arm64-v8a.apk`**. El universal vale para otros Android.

**2. Activa el modo desarrollador.** Solo hay que hacerlo una vez.
1. Crea o únete a una organización en [developers.meta.com](https://developers.meta.com/horizon/). Es gratis.
2. En el móvil, abre la app **Meta Horizon** y ve a **Dispositivos → tus Quest → Ajustes del visor → Modo desarrollador**. Actívalo.
3. Reinicia las gafas.

**3. Instala la app.** Elige una de estas dos formas:
- **SideQuest** (lo más fácil):
  1. Conecta las Quest al PC por USB y acepta **"Permitir depuración USB"** dentro de las gafas.
  2. En [SideQuest](https://sidequestvr.com/setup-howto), arrastra el APK a la ventana o usa el botón *Install APK*.
- **adb**:
  ```bash
  adb devices                      # acepta el aviso dentro de las gafas
  adb install -r SmartTube_*_arm64-v8a.apk
  ```

**4. Ábrela.** En las Quest, ve a **Biblioteca → Apps → Orígenes desconocidos** y abre **SmartTube VR**.

> ℹ️ Se instala como app propia (`app.smarttube.fdroid.vr`), así que **no sustituye** tu SmartTube normal.
> 🔄 **Actualizaciones**: a partir de esta versión, todas las compilaciones se firman con la **misma clave de SmartTube VR**, así que las nuevas versiones **se instalan encima** sin perder tus ajustes.
> ⚠️ **Una única vez**: si vienes de una versión de prueba anterior, que estaba firmada con otra clave, **desinstálala antes** de instalar esta. Guarda antes tus ajustes con la copia de seguridad de SmartTube.
>
> Huella SHA-256 del certificado oficial, para comprobarla con `apksigner verify --print-certs`:
> `0b0d0a877deb95d20ea099a10d34231bcc4068dbf3bf22b090cb959edb4860b0`
> Las compilaciones hechas en *forks*, sin los secretos de firma, usan una clave *debug* temporal y no se instalan encima de las oficiales.

---

## 🔧 Cómo se construye

Este repositorio **no copia ni reimplementa YouTube ni SmartTube**. Solo contiene parches pequeños:
1. GitHub Actions descarga el código oficial de [yuliskov/SmartTube](https://github.com/yuliskov/SmartTube), fijado en el commit `ffa16426`.
2. Aplica los parches de [`patches/`](patches/).
3. Compila la variante `stfdroidDebug`.
4. Firma con la clave estable de SmartTube VR, que se guarda en los secretos de GitHub Actions, y comprueba que la huella del certificado coincide. Revisa también las librerías ARM64, el paquete `.vr`, el inicio y que no exija Leanback ni pantalla táctil.

| Parche | Qué hace |
|---|---|
| `smarttube_adstop.py` | Perfiles AdStop de SponsorBlock, accesos rápidos de DeArrow con arreglos de robustez, y nombre y paquete propios. |
| `smarttube_buttons_visual.py` | Retoques de contraste en botones y tarjetas. Solo XML. |
| `smarttube_cinema.py` + `cinema/` | Escenarios y controles de cine alrededor del reproductor original. |
| `smarttube_home.py` + `home/` | El inicio tipo launcher (`QuestHomeActivity`). |

Cada parche comprueba todos sus anclajes **antes** de escribir nada y falla si upstream cambia.

```bash
# Pruebas
PYTHONPATH=. python3 -m unittest discover -s tests
javac -d /tmp/t patches/cinema/CinemaGeometry.java tests/CinemaGeometryTest.java && java -cp /tmp/t CinemaGeometryTest
javac -d /tmp/h patches/home/HomeTiles.java tests/HomeTilesTest.java && java -cp /tmp/h HomeTilesTest
# Regenerar los renders de docs/
javac -d /tmp/r patches/cinema/CinemaGeometry.java tools/previews/RenderPreviews.java && java -Djava.awt.headless=true -cp /tmp/r RenderPreviews docs/escenarios
```

## ⚠️ Límites honestos
- Los escenarios se dibujan en la **ventana 2D de Android** de las Quest. No es una app OpenXR: no hay 3D estereoscópico ni seguimiento de cabeza.
- Netflix, Disney+ y servicios parecidos se abren en Brave o en el navegador, no dentro del cine.
- SponsorBlock y DeArrow dependen de sus servidores comunitarios y no garantizan quitar toda la publicidad.
- Es una compilación no oficial y experimental. Pruébala bajo tu responsabilidad.

## 🙏 Créditos
- **[SmartTube](https://github.com/yuliskov/SmartTube)**, de [yuliskov](https://github.com/yuliskov) y sus colaboradores: todo el reproductor, la navegación y los servicios. Si te gusta, apoya el proyecto original.
- **[SponsorBlock](https://sponsor.ajay.app/)** y **[DeArrow](https://dearrow.ajay.app/)**, de Ajay Ramachandran y su comunidad.
- Los parches de SmartTube VR son de [pilahito](https://github.com/pilahito).

## 📄 Licencia
[MIT](LICENSE), la misma licencia que el SmartTube original. Se conserva el aviso de copyright de yuliskov.

SmartTube VR **no está afiliado** a Google, YouTube, Meta ni al proyecto SmartTube oficial.
