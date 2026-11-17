# Clasificación y desconexiones — Nightminigames Fabric 1.21.1

## Reglas de la región Salida

La clasificación de cada equipo se evalúa cuando su máquina ha alcanzado 4/4 y existe una región Salida configurada para ese equipo.

- Solo se contabilizan jugadores conectados, vivos y en Supervivencia o Aventura.
- Los jugadores desconectados, en Espectador o en Creativo no impiden el cierre ni la clasificación del resto de su equipo.
- Todos los jugadores elegibles de un equipo deben estar dentro de SU región Salida al mismo tiempo.
- Debe existir al menos un jugador elegible. Si todos se desconectan o están en Espectador, el equipo no clasifica automáticamente y la puerta permanece a la espera.
- Al cumplirse la condición, se oculta el título de máquina completada y la puerta de Salida puede cerrarse de forma segura.
- Si los cupos de clasificación ya están completos, el equipo puede terminar su recorrido, pero no ocupa una plaza adicional.
- Los equipos clasificados siguen clasificados si sus integrantes se desconectan o cambian de modo DESPUÉS de clasificar.

## Cupos de clasificación

Los cupos son de **equipos**, no de jugadores. El valor predeterminado es 7 y se puede configurar de 1 a 7.

```mcfunction
/nightminigames clasificacion cupos 4
/nightminigames clasificacion estado
/nightminigames clasificacion lista
```

Los cupos de jugadores por equipo siguen administrándose aparte:

```mcfunction
/nightminigames equipos cupos todos 20
/nightminigames equipos estado
```

No es posible reducir los cupos de clasificación por debajo de la cantidad de equipos que ya clasificaron. Reiniciar la máquina de uno de los equipos clasificados libera su plaza y permite una nueva clasificación.

## HUD y persistencia

Al clasificar un equipo aparece una notificación permanente de una línea en el HUD global, por ejemplo:

`EQUIPO ROJO CLASIFICÓ · 1/4 · FALTAN 3`

Al cambiar los cupos se muestra:

`CLASIFICACIÓN 0/4 · FALTAN 4`

La clasificación, su orden de llegada, los cupos y el progreso de salida se guardan por mundo en:

`world/data/nightminigames_team_exit.json`

Se conservan los datos anteriores de salida. Al abrir por primera vez una partida antigua, las sesiones previamente marcadas como llegadas se migran a la clasificación mientras queden plazas.

## Casos para comprobar

1. Equipo de cuatro: dos en Salida, un desconectado, un espectador → el equipo clasifica si los dos activos están en Salida.
2. Equipo de cuatro: tres desconectados, un activo fuera de Salida → no clasifica.
3. Equipo de cuatro: los cuatro desconectados → no clasifica ni cierra por clasificación.
4. Cuatro cupos, cuatro equipos ya clasificados, quinto equipo llega → no obtiene cupo.
5. Cuatro cupos, tres equipos clasificados, cuarto llega → ocupa el cuarto y el HUD muestra FALTAN 0.
