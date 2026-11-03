# ScrollParty — decisiones de producto (grill del 2026-11-03)

## Idea
Un grupo de amigos va metiendo reels (Instagram), TikToks y YouTube Shorts en una **bolsa compartida**.
Cuando quedan, un **anfitrión** conecta su móvil a la tele (AirPlay / duplicar pantalla) y los ven
uno a uno. Antes de revelar cada uno, el resto **adivina quién lo mandó**. Al final hay ranking y premios.

## Nombre y mercado
- Nombre: **ScrollParty**. Público internacional.
- UI en **inglés por defecto**, localizada al **español**. Docs internas en español.

## Diferencial
- Apps parecidas (WhoLiked, Scrolled) hacen adivinar a quién *le gustó* un vídeo a partir de los likes.
  En ScrollParty cada uno **elige y manda** sus vídeos a propósito, se ven juntos **en la tele** y hay premios.

## Plataformas
- **iOS nativo** (Swift + SwiftUI) y **Android nativo** (Kotlin + Jetpack Compose).
- Mismo backend para ambos: jugadores de iOS y Android juegan en el mismo grupo.
- Orden: iOS primero (Santi tiene Mac + iPhone), Android después reutilizando modelo de datos y reglas.

## Grupo
- Se crea un grupo y se entra con un **código** (sin registro: nombre + emoji).
- El creador es **admin**. Si el admin se sale, pasa al miembro más antiguo.
- El grupo vive hasta que todos se salen o el admin lo cierra.
- **Nueva partida** (admin): vacía la bolsa y archiva la partida anterior.
- Se guarda un **historial de partidas** con la lista de enlaces de cada una.

## Bolsa
- Se añaden enlaces (no se descargan vídeos: no es posible ni legal). Vía Share Extension /
  intent de compartir ("Compartir → app") y pegando enlace a mano.
- **Límite de reels por persona configurable** por los jugadores al crear la partida (opcional).
- Nadie ve los reels de los demás antes de la sesión; solo cuántos ha mandado cada uno.
- En la sesión salen en **orden aleatorio**.

## Sesión (modo tele)
- El anfitrión controla el avance con **"Siguiente"** (manual, nunca automático).
- Reproducción: reproductor embebido cuando la plataforma lo permita (YouTube, TikTok);
  si no (Instagram), abrir en la app oficial y volver. *A validar con un prototipo.*
- Cada jugador **vota desde su propio móvil** quién lo mandó. El autor no vota en el suyo.
- La tele muestra "N/M han votado"; cuando votan todos (o el anfitrión fuerza), se revela el autor.
- Tras cada reel, reacción rápida: 😂 / 💀 / 🔥 / 😴.

## Puntuación y premios
- +1 por acertar el autor. +1 al autor por cada amigo que falla.
- 🕵️ **Detective**: más aciertos.
- 🥷 **Infiltrado**: más gente engañada.
- 💀 **Reel de la noche**: más reacciones 😂/💀.

## Backend (propuesta)
- Firebase: Auth anónima + Firestore (tiempo real para votos y estado de la sesión).
  Elegido sobre CloudKit porque es multiplataforma (Android) y no exige iCloud.

## Fuera del MVP
- Descargar o recortar vídeos, comentarios, chat, cuentas con email, notificaciones push.
