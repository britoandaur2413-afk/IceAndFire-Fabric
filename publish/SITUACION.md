# Ice and Fire Fabric para 1.20.1

**En corto:** el mismo mod (dragones, criaturas, el hada exiliada, la ciudadela y el duelo de Cryona), ahora para
**Minecraft 1.20.1**. Publicada la 1.0.3-beta (27-10-2026) en Modrinth (`1.0.3-beta+1.20.1`) y CurseForge.

## Qué hay en esta carpeta

| Archivo | Para qué |
|---|---|
| `IceAndFire-Fabric-1.20.1-1.0.3-beta.jar` | El mod. Va en `mods` junto a Fabric API (Minecraft 1.20.1, Java 17 o más). |
| `IceAndFire-Fabric-1.20.1-1.0.3-beta-sources.jar` | Su código fuente (la licencia pide que esté junto al mod). |
| `LICENSE.txt`, `CREDITS.md`, `icono.png` | Los mismos de siempre. |

## 1.0.1-beta (lo que se vio al jugarlo)

- **Ya no se cierra junto a otros mods** que añaden métodos con el mismo nombre al juego (ShinColle 1.0.1 lo hacía:
  "Ticking entity" al primer pollo). Los de Ice and Fire llevan ahora el prefijo del mod.
- **Las alas** de los dragones (y todo modelo con partes planas) se ven también por debajo.
- **El reino de Cryona** está en penumbra como en 26.3: el suelo de nieve ya no sale blanco y los círculos de aviso se
  ven bien.
- **Sin espirales delante de los ojos:** las partículas de tus propios efectos (el frío de Black Frost) no se dibujan
  en primera persona; parpadeaban sobre los círculos.
- Black Frost ya no sigue "echando aliento" al empezar el duelo.

## Qué cambia respecto a 1.21.1

- **Etiquetas comunes:** usa los nombres de 1.20.1 (`c:silver_ingots`, `c:sapphires`...), así las recetas aceptan la
  plata y los zafiros de otros mods de 1.20.1.
- **F1** oculta también los avisos del mod en pantalla, como hace el juego en 1.20.1.
- Lo demás es igual: el cobre del original, los mismos textos en 16 idiomas, el mismo menú principal, las mismas escenas.

## Arreglado de paso

- Black Frost podía seguir "echando aliento" unos segundos al empezar el duelo, sin atacar. Ya no. (Pasa también en
  26.3 y 1.21.1; ahí no lo toqué.)

## Qué está probado

- Las 58 pruebas del servidor pasan.
- Las 13 escenas del cliente pasan, con 371 capturas (las mismas que en 1.21.1).
- Con el jar final en un Minecraft 1.20.1 real (el de CurseForge, sin la herramienta de desarrollo).

## Para publicarlo

Se sube como **otro archivo** en los mismos proyectos de CurseForge y Modrinth (versión de juego 1.20.1, Fabric,
Fabric API obligatorio). Dime y lo subo.

## 1.0.2-beta

- **Black Frost y Cryona no se vuelven piedra:** ni la mirada de una gorgona, ni su cabeza usada como arma, ni una cocatriz pueden petrificarlos (el duelo no se acababa de un vistazo).

## 1.0.3-beta (la update 1.0.2-beta de 26.3, publicada el 27-10-2026)

- **Silla de montar doble:** un amigo va detrás del dueño en un dragón de fuego, hielo o relámpago desde la fase 4.
- **Aliento nuevo:** el fuego, la escarcha y el relámpago salen de la boca del dragón.
- **Jinetes en el lomo,** que se mueven con el dragón; las cajas de golpe siguen su modelo en cada pose.
- **Clic derecho en cualquier parte del dragón** (cabeza, cuello, cuerpo, alas o cola) para montarlo, darle de comer o
  abrir su menú. Shift + clic derecho abre el menú con cualquier cosa en la mano; se puede montar con una espada.
- Shift en el aire: el dragón aterriza contigo encima. El ojo del cíclope recibe flechas de frente.
- La pantalla de bienvenida de GAMITA_MODELS, una vez antes del menú principal (sin expansión anunciada).
- Probado: 67 de 68 pruebas del servidor (la del oráculo falla a veces desde antes y pasa sola), las escenas de la
  silla y del aliento, y en un Minecraft 1.20.1 real junto a Sodium, Iris y los shaders Complementary.
