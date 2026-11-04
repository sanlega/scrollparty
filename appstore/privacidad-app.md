# App Privacy y edad

Respuestas basadas en el código actual. Firebase Auth anónimo y Firestore; sin analítica, publicidad ni rastreo.

## App Privacy
**¿Recopila datos?** Sí. **¿Rastreo (tracking)?** No.

| Tipo en Apple | Dato | Uso | ¿Vinculado al usuario? |
|---|---|---|---|
| Contact Info › Name | Apodo elegido | App Functionality | Sí |
| User Content › Photos or Videos | Selfie opcional, 256 px | App Functionality | Sí |
| User Content › Other User Content | Enlaces, pistas, votos y reacciones | App Functionality | Sí |
| Identifiers › User ID | Identificador anónimo de Firebase | App Functionality | Sí |
| Identifiers › Device ID | Identificador de instalación de Firebase, si el SDK lo envía | App Functionality | Sí |

Verifica el último punto contra el Privacy Manifest del SDK de Firebase de la build.

## Cifrado
`ITSAppUsesNonExemptEncryption = false`: solo HTTPS estándar.

## Permisos del dispositivo
Cámara: selfie opcional. Fototeca: mediante selector del sistema.

## Cuestionario de edad (propuesta)
| Pregunta | Respuesta |
|---|---|
| Violencia, sexo, drogas, terror | None |
| Lenguaje obsceno o humor crudo | None |
| Juegos de azar y simulados | None: no hay dinero, apuestas ni premios reales |
| Concursos | None |
| Navegación web sin restricciones | No: los enlaces abren en apps externas |
| Contenido generado por usuarios | Yes |
| Mensajería o chat | No: solo pistas anónimas de hasta 80 caracteres |
| Publicidad | No |

La edad resultante la calcula Apple; revísala con `PENDIENTES.md`, punto 1.
