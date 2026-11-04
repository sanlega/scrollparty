# Pendientes antes de enviar a revisión

## Bloqueos probables de Apple
1. **Contenido generado por usuarios (guideline 1.2).** Selfies, nombres, pistas y enlaces. La app no tiene hoy
   denunciar contenido, bloquear jugadores ni filtro. Los grupos son privados por código, pero Apple puede exigirlo igualmente.
   Requiere cambios en la app.
2. **Borrar datos (guideline 5.1.1).** No hay registro, pero sí datos en Firestore. «Leave party» solo olvida el grupo
   en ese móvil: la selfie, el nombre y los enlaces siguen guardados y no caducan. Hay que implementar salir de verdad,
   borrar datos o caducidad de grupos (ticket 005). La política actual lo declara con esa limitación.
3. **Revisión con un solo dispositivo.** Empezar exige 2 jugadores y solo el anfitrión puede empezar. Ver `app-review.md`.
   Conviene un modo demo o un grupo preparado.
4. **Enlaces de terceros (guideline 5.2).** La app abre vídeos de otras plataformas en sus apps oficiales. No descarga ni
   embebe nada. Por eso los textos no nombran Instagram, TikTok ni YouTube, y tampoco deben ir en palabras clave ni capturas.

## Decisiones de Santi
- **Email de contacto público:** sustituir `CONTACT_EMAIL` en `legal/` y `app-review.md`.
- **Responsable legal:** confirmar nombre en `legal/privacidad.md`, y dirección si la exige tu asesor.
- **Revisión legal:** las políticas son un borrador, no asesoramiento jurídico.
- **Edad:** el cuestionario propuesto está en `privacidad-app.md`. Si hay contenido de usuarios, puede subir.
- **iPad (recomiendo solo iPhone para la v1):** la app se publica también para iPad, pero allí solo se estira y deja
  mucho vacío, y Apple lo puede valorar como mala experiencia. Para solo iPhone: `TARGETED_DEVICE_FAMILY` a `"1"` en la app
  y la extensión, otra build, y borrar `screenshots/*/ipad-*`. La alternativa es limitar el ancho del contenido en iPad.
- **Versión:** la build es 0.1.0. Para publicar, conviene subir `MARKETING_VERSION` a `1.0.0`.

## Comprobaciones pendientes
- El onboarding y el campo de añadir enlace dicen «TikTok» en la app. Las capturas evitan esas pantallas; conviene
  cambiar esos textos por «reel o vídeo corto» en la app antes de enviar.
- Probar en iPhone real: selfie con cámara, extensión de compartir y partida completa.
- Las capturas son de la app real en simulador, en modo demo y con personajes dibujados.
- La URL de privacidad apunta a `main`: `appstore/` debe estar fusionada antes de enviar.
