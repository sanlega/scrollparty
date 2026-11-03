# Poner ScrollParty en móviles reales

Hasta completar esto, las apps solo funcionan contra el Firebase Emulator del PC.

## 1. Firebase (una vez)
1. Crea el proyecto en https://console.firebase.google.com (plan Spark, gratuito).
2. **Authentication → Sign-in method:** activa **Anónimo**.
3. **Firestore Database:** crea la base de datos en modo producción (región `eur3`).
4. **Configuración del proyecto:** añade una app iOS (`com.santilega.scrollparty`) y una Android (mismo id).
   De ellas salen los valores de abajo; no hace falta descargar los ficheros de configuración.
5. Sube las reglas:
   ```bash
   cd firebase
   npx firebase login
   npx firebase deploy --only firestore:rules --project <ID_DEL_PROYECTO>
   ```

## 2. GitHub: variables y secrets
Las claves cliente de Firebase no son secretas (las protegen las reglas), así que van como variables:
```bash
gh variable set FIREBASE_API_KEY --body "..."
gh variable set FIREBASE_PROJECT_ID --body "..."
gh variable set FIREBASE_IOS_APP_ID --body "..."
gh variable set FIREBASE_SENDER_ID --body "..."
```
Los secrets de Apple son los mismos valores que en `42intra-ios`. GitHub no deja leerlos, hay que volver a darlos:
`ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_API_KEY` (contenido del `.p8`), `APPLE_TEAM_ID`, `MATCH_PASSWORD`.
```bash
gh secret set ASC_API_KEY < AuthKey_XXXX.p8
```

## 3. TestFlight
1. En App Store Connect crea la app con el bundle id `com.santilega.scrollparty`.
   La API de Apple no permite crearla; el bundle id sí lo registra el pipeline, pero antes hay que tenerlo
   para este paso: créalo en developer.apple.com → Identifiers si aún no sale en la lista.
2. Lanza el workflow: `gh workflow run ios-testflight.yml -f changelog="Primera beta"`.

## Android contra el proyecto real
```bash
cd android
FIREBASE_API_KEY=... FIREBASE_PROJECT_ID=... FIREBASE_APP_ID=<id de la app Android> ./gradlew assembleDebug
```
