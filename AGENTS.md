# ScrollParty
App social de fiesta: un grupo mete reels/TikToks/Shorts en una bolsa compartida y en la quedada
los ven en la tele mientras adivinan quién mandó cada uno. Spec de producto: `docs/spec/producto.md`.

## Stack
- `ios/` — Swift + SwiftUI (nativo). Se compila en el Mac de Santi (Xcode); la máquina de desarrollo de Claude es Linux.
- `android/` — Kotlin + Jetpack Compose (nativo). Va después de iOS.
- `firebase/` — reglas de Firestore y config compartida por ambas apps.

## Flujo de trabajo
- Idea difusa → grill. Duda de UX/lógica → prototipo desechable. Trabajo de varias sesiones → spec + tickets.
- Tickets y specs en ficheros locales: ver `docs/agents/issue-tracker.md`.
- Decisiones relevantes en `docs/decisiones/` (una por fichero, fechadas).
- Idioma: docs y commits en español. UI en inglés por defecto + localización al español.

## Verify
- ./scripts/verify   (rellenar: typecheck + tests + build del stack)

## Gotchas
- No se descargan vídeos: solo se guardan enlaces. Instagram no se embebe bien → validar reproducción con prototipo.
- La tele es duplicado de pantalla (AirPlay / Cast), no hay que implementar casting.
- iOS no se puede compilar desde Linux: hace falta el Mac de Santi (o acceso SSH a él).
