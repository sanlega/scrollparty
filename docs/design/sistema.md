# Design system «La Feria»

**Concepto:** ScrollParty es una recreativa de feria. Los reels entran en la máquina, las caras de tus amigos
giran en el rodillo y los puntos son tiques. Prohibido el lenguaje de casino: nada de bet, jackpot, cash, spin, win big.
Referencia viva: rama `prototype/rediseno-direcciones`, `docs/design/prototipo/jackpot.html`.

## Tokens (iguales en iOS y Android)

### Color
| Token | Valor | Uso |
|---|---|---|
| `blue` | `#2323FF` | Fondo general |
| `blueDeep` | `#1616C9` | Rayos del fondo, sombra de texto display |
| `night` | `#0B0B3B` | Fondo donde el vídeo es protagonista (tele, voto secreto) |
| `night2` | `#17175C` | Botón desactivado |
| `yellow` | `#FFE600` | Máquina, botón primario, display |
| `yellowDeep` | `#B3A000` | Labio del botón y de la máquina |
| `coral` | `#FF4D2E` | Barra de selección, luces. Nunca con texto encima |
| `mint` | `#16E0A6` | Acierto, confirmado (texto siempre `ink`) |
| `blush` | `#FFC9BF` | Tique «Fooled» (texto `ink`) |
| `white` | `#FFFFFF` | Ventana de la máquina, tarjetas |
| `ink` | `#14143C` | Texto sobre claro |
| `inkSoft` | `#4A4A75` | Texto secundario sobre claro; opciones no centradas |
| `soft` | `#D9D9FF` | Texto secundario sobre azul o noche |

Colores de jugador (índice `color` 0-11): `#FF4D2E #16E0A6 #FF4FD8 #FFB400 #2FB6FF #FFE600 #B06BFF #7BEA3A #FF7A59 #3DE0E0 #FF5C9E #FFC14D`.

Reglas: texto 4,5:1 y controles 3:1 como mínimo. Un test comprueba los pares usados. Nunca se baja la opacidad
para desactivar. Blanco sobre `coral` está prohibido.

### Tipografía
Dos familias OFL empaquetadas: **Bowlby One** (display) y **DM Sans** Medium/Bold (el resto).

| Rol | Fuente | Tamaño/línea |
|---|---|---|
| `display` | Bowlby One, mayúsculas | 46/48 |
| `title` | Bowlby One, mayúsculas | 34/36 |
| `button` | Bowlby One, mayúsculas | 21 |
| `heading` | Bowlby One | 22/24 |
| `body` | DM Sans Bold | 19/27 |
| `label` | DM Sans Bold, mayúsculas, tracking 0.06em | 14 |
| `caption` | DM Sans Medium | 14 |

Todo escala con el tamaño de texto del sistema. Los titulares se ajustan solos sin cortarse (`minimumScaleFactor`
0.6 en iOS; autosize en Compose) y nunca llevan saltos manuales: el español es más largo. Bowlby One no tiene
`→ ✓`: esos símbolos son iconos del sistema.

### Espacio y forma
Espaciado múltiplo de 8: 8, 16, 24, 32. Margen de pantalla 24. Radios: 14, 22, 30.
Zona táctil mínima 48. Botón primario 64 de alto. Respeta la safe area; sin alturas fijas de pantalla.

### Movimiento
| Token | Curva o muelle | Uso |
|---|---|---|
| `spring` | rebote: `cubic-bezier(.3,1.7,.5,1)` | Aparición, selección |
| `snap` | `cubic-bezier(.2,.8,.2,1)`, 320 ms | Hojas, transiciones |
| `press` | 80 ms | Botón que se hunde |
| `reel` | `cubic-bezier(.15,.7,.2,1.04)`, 1500 ms | Rodillo del revelado |

Un único punto de movimiento en cada plataforma que lee el ajuste de movimiento reducido
(`accessibilityReduceMotion`; `ANIMATOR_DURATION_SCALE` en Android). Con él: sin rodillo, confeti ni rayos giratorios;
fundidos de 150 ms y las reacciones se muestran como contador. La háptica se mantiene.

## Componentes
`PrimaryButton` (primaria y `alt` blanca, con labio de 7 que se hunde al pulsar; estados normal, pulsado, desactivado, cargando) ·
`Machine` (carcasa amarilla con labio, luces opcionales y ventana blanca) · `SunBackground` (rayos vectoriales) ·
`PlayerFace` (selfie a dos tonos o iniciales sobre el color del jugador) · `SuspectWheel` · `Ticket` (muescas laterales) ·
`ReelReveal` · `ReactionBar` · `FlyingReactions` · `StepBar` · `Toast` · `ClueCard` · `PrizeCard` · `DisplayTitle`.

**PlayerFace:** la foto en escala de grises multiplicada por el color del jugador, con trama de puntos encima.
Así cualquier selfie encaja. Sin foto: iniciales en `ink` sobre el color.

## Pantallas
| Pantalla | Fondo | Quién |
|---|---|---|
| Onboarding, 4 pasos | `blue` | Primer arranque; se puede saltar |
| Selfie y nombre | `blue` | Antes de crear o unirse |
| Inicio: crear o unirse | `blue` | Todos |
| Máquina (bolsa) | `blue` | Código, invitar, contador, jugadores, añadir reel con pista, empezar (anfitrión) |
| Tele | `night` | Anfitrión: vídeo grande, «N of M voted», voto secreto, reacciones volando, revelar |
| Voto secreto | `night` | Anfitrión: rejilla neutra; al votar vuelve con «Your vote is in · Change» sin indicar a quién |
| Elegir | `blue` | Votantes: pista, rueda (tocar o scroll), reacciones, «That’s my guess» y cambiar hasta revelar |
| Es tuyo | `blue` | Autor: su cara, sin voto, reacciones |
| Revelado | `blue` | Rodillo y resultado, tique +1, quién votó qué. «Next reel» solo anfitrión |
| Final | `blue` | Tiques, premios Top Detective, Master Bluffer y Reel of the Night |

El anfitrión nunca ve resaltada su selección: el mismo contador y mensaje aparecen sea quien sea el elegido.

## Copys clave
| EN | ES |
|---|---|
| Feed the machine | Alimenta la máquina |
| Who sent it? | ¿Quién lo mandó? |
| That’s my guess | Es mi respuesta |
| Reveal who sent it | Revelar el autor |
| This one’s yours | Este es tuyo |
| Your vote is in · Change | Voto registrado · Cambiar |
| Fooled | Engañado |
| Top Detective | Mejor detective |
| Master Bluffer | Rey del engaño |
| Reel of the Night | Reel de la noche |
