# Nightminigames — Guardado automático y desasignación

## Guardado automático

No se necesita `/nightminigames persistencia guardar` y ese subcomando se eliminó. El índice de estructuras y las plantillas de los siete equipos se actualizan automáticamente como máximo cada 100 ticks del servidor si cambian y al detenerse normalmente. Los demás sistemas conservan sus escrituras JSON automáticas. La configuración deliberada de puntos, destinos y kits sigue teniendo sus acciones específicas para *definir* dichos datos.

Las asignaciones de equipos se almacenan en el scoreboard persistente de Minecraft y también aparecen en `world/data/nightminigames_structures.json` bajo `teams`. Los nombres desconectados permanecen en el scoreboard hasta que un OP los desasigne.

## Comandos OP

- `/nightminigames equipos quitar <jugador>` — admite un nombre desconectado.
- `/nightminigames equipos desasignar <jugador>` — alias.
- `/nightminigames equipos vaciar <equipo>` — quita a todos los integrantes de ese equipo, conectados y desconectados.
- `/nightminigames equipos vaciar todos` — desasigna a todos de los siete equipos sin borrar los equipos, las regiones, los módulos ni sus kits.
- `/nightminigames equipos estado` — muestra ocupación actual, incluidos los miembros desconectados.
- `/nightminigames persistencia estado` — consulta el estado del autoguardado sin escribir manualmente.

El vaciado modifica únicamente la membresía actual de los equipos; no borra el historial de clasificados ni revoca puntos de rondas anteriores. Para reiniciar una ronda utiliza `/nightminigames inicio reiniciar` por separado.
