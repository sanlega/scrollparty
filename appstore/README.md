# Kit de la App Store de Scroll Party

Todo lo necesario para configurar y publicar la ficha. **Lee primero `PENDIENTES.md`:** hay bloqueos que no son de ficha.

## Datos de la app
| Campo | Valor |
|---|---|
| App en App Store Connect | `Scroll Party`, Apple ID `6830396279` |
| Bundle id | `com.santilega.scrollparty` |
| Extensión | `com.santilega.scrollparty.share` |
| SKU | `scrollparty` |
| Team ID | `6Z5BUPQ376` |
| Build en TestFlight | versión 0.1.0, build `202611040320` |
| Idioma principal | English (U.S.) |
| Localizaciones | `en-US`, `es-ES` |
| Categoría principal | Games, subcategorías Family y Trivia |
| Secundaria | Entertainment |
| Precio | Gratis |
| Disponibilidad | Todos los países, salvo decisión contraria |
| Copyright | `2026 Santi Legaristi` |

El nombre visible en el móvil es `ScrollParty`; en la tienda, `Scroll Party`.

## Contenido
| Ruta | Qué es |
|---|---|
| `metadata/<locale>/` | Textos por idioma, en el formato de `fastlane deliver` |
| `screenshots/<locale>/` | Capturas en el formato de `fastlane deliver`; prefijo `iphone-` 1320×2868, `ipad-` 2064×2752 |
| `screenshots/*/` | 6 de iPhone 6,9" y 4 de iPad 13" por idioma, ya compuestas |
| `icon/` | Icono de 1024×1024, sin transparencia |
| `app-review.md` | Notas para el revisor de Apple |
| `privacidad-app.md` | Respuestas de App Privacy y cuestionario de edad |
| `legal/` | Política de privacidad y soporte, con su URL pública |
| `PENDIENTES.md` | Lo que debe resolverse antes de enviar |

## Cómo subirlo
Credenciales: la API key de App Store Connect ya está en los secrets del repo como `ASC_KEY_ID`, `ASC_ISSUER_ID` y `ASC_API_KEY`.

```bash
cd appstore
fastlane deliver --app_identifier com.santilega.scrollparty \
  --metadata_path metadata --screenshots_path screenshots \
  --skip_binary_upload true --submit_for_review false --force true
```

No envíes a revisión hasta que `PENDIENTES.md` esté resuelto y Santi lo apruebe.
