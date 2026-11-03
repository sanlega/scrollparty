# 2026-11-04 — Modelo de datos y lógica sin Cloud Functions

**Decisión:** todo vive en Firestore y la autoridad de la partida es el móvil del anfitrión (el admin).
Sin Cloud Functions, para quedarnos en el plan gratuito Spark. Las dos apps implementan este contrato.

```
groups/{code}            adminId, status (collecting|playing|finished), roundIds[], roundIndex
  members/{uid}          name, emoji, count (enlaces en la bolsa), joinedAt
  links/{id}             url, authorId          ← solo los ve su autor; en sesión, también el anfitrión
  rounds/{linkId}        url, authorId (null hasta revelar), votes {uid: uidAdivinado}
```

- El **código** de 6 caracteres es el id del grupo.
- Una ronda usa el **mismo id que su enlace**: así cada móvil sabe que el reel es suyo (y no vota) sin que
  `authorId` viaje antes de tiempo.
- **Empezar:** el anfitrión pone `playing`, lee los enlaces, baraja y crea las rondas. Si falla a medias, puede reintentar.
- **Revelar:** el anfitrión copia `authorId` a la ronda; desde ahí las reglas ya no aceptan votos.
- **La puntuación no se guarda:** cada móvil la calcula a partir de las rondas reveladas.
- Votos esperados en una ronda: miembros − 1 (el autor no vota).

**Coste asumido:** el móvil del anfitrión tiene acceso a los autores, así que un anfitrión con conocimientos
podría hacer trampa, y otro miembro podría escribir un `count` falso. Para un juego entre amigos no compensa
blindarlo con Cloud Functions.
