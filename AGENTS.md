# ScrollParty
App social de fiesta: un grupo mete reels/TikToks/Shorts en una bolsa compartida y en la quedada
los ven en la tele mientras adivinan quién mandó cada uno. Spec de producto: `docs/spec/producto.md`.

## Stack
- `ios/` — Swift + SwiftUI (nativo), proyecto generado con XcodeGen. Se compila en GitHub Actions; la máquina de desarrollo es Windows.
- `android/` — Kotlin + Jetpack Compose (nativo). Se prueba en los emuladores de este PC.
- `firebase/` — reglas de Firestore, sus tests y el Firebase Emulator (Auth 9099, Firestore 8180).
- Contrato de datos común: `docs/decisiones/2026-11-04-modelo-de-datos.md`. Puesta en producción: `docs/DEPLOY.md`.

## Flujo de trabajo
- Idea difusa → grill. Duda de UX/lógica → prototipo desechable. Trabajo de varias sesiones → spec + tickets.
- Tickets y specs en ficheros locales: ver `docs/agents/issue-tracker.md`.
- Decisiones relevantes en `docs/decisiones/` (una por fichero, fechadas).
- Idioma: docs y commits en español. UI en inglés por defecto + localización al español.

## Verify
- `./scripts/verify`: reglas de Firestore + tests y APK de Android (con los emuladores de Firebase parados).
- `ANDROID_SERIAL=emulator-5554 ./scripts/e2e-android`: partida completa en un emulador Android encendido.
- iOS: workflow `ios-build-check.yml`, se lanza en cada PR que toque `ios/`.

## Gotchas
- No se descargan vídeos: solo se guardan enlaces. Instagram no se embebe bien → validar reproducción con prototipo.
- La tele es duplicado de pantalla (AirPlay / Cast), no hay que implementar casting.
- iOS no se puede compilar desde Windows: cada cambio se comprueba en GitHub Actions, que en repo privado gasta minutos de macOS (cuentan x10).
- El puerto 8080 del PC está ocupado por otro proceso: por eso Firestore usa el 8180.
