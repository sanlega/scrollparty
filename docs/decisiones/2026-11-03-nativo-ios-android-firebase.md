# 2026-11-03 — Nativo iOS + Android, backend Firebase

**Decisión:** dos apps nativas (SwiftUI y Jetpack Compose) sobre un mismo backend Firebase.

**Por qué:** Santi quiere la mejor calidad de app en cada plataforma y que jueguen todos los amigos,
tengan iPhone o Android. Firebase es multiplataforma, da tiempo real para votos y auth anónima
para entrar con código sin registro. CloudKit se descartó porque obliga a iCloud y deja fuera Android.

**Coste asumido:** doble UI. Se mitiga haciendo iOS primero y portando a Android con el mismo modelo de datos.
