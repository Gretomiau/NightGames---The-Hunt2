# Inicio global, diagnóstico y reinicio de Nightminigames

## Comandos OP

- `/nightminigames inicio estado`: muestra un resumen breve del estado global.
- `/nightminigames inicio diagnostico`: muestra el diagnóstico completo de destinos, cupos, kits y puertas.
- `/nightminigames modulos estado`: muestra un resumen compacto de los siete módulos.
- `/nightminigames modulos diagnostico`: muestra la configuración detallada.
- `/nightminigames modulos teleportar todos`: envía a los jugadores de Survival y Adventure a sus módulos.
- `/nightminigames inicio comenzar`: inicia globalmente las siete puertas de Entrada después de la distribución.
- `/nightminigames inicio reiniciar`: cancela la distribución pendiente, bloquea el inicio, restablece la clasificación y permite entregar nuevamente kits.

## Comportamiento de las puertas

- Una puerta de Entrada no abre por ocupación antes del comando de inicio.
- Al comenzar, cada puerta autorizada abre y, tras el paso de los jugadores, se cierra cuando su región quede vacía.
- No se cierra si hay participantes atravesando su estructura.
- Al reiniciar el servidor, la autorización temporal de apertura no se restaura automáticamente.

## Reinicio de estructuras

El reinicio no elimina regiones, equipos, cupos, kits, destinos ni puertas colocadas. El progreso de las máquinas y el estado de recogida de los spawners de llaves y linterna se restablecen sin cargar chunks forzosamente.

El archivo `data/nightminigames_round_generation.json` guarda el contador de reinicios. Cada máquina y spawner guarda la última ronda aplicada. Las estructuras que estén en chunks descargados se restablecen cuando dichos chunks vuelvan a cargarse. Se procesan como máximo ocho estructuras por tick.

Los dos historiales HUD se vacían con el reinicio de la ronda. Los cupos de clasificación se conservan, pero las plazas se liberan.

El reinicio no teletransporta a los jugadores ni elimina sus ítems. Para una siguiente ronda, administra los inventarios y ejecuta de nuevo el teletransporte global antes del inicio.

## Pruebas recomendadas

1. Colocar una puerta Entrada y asignarle una región, entrar en la región y verificar que la puerta siga cerrada.
2. Repartir participantes a módulos y ejecutar `inicio comenzar`; verificar apertura y cierre tras abandonar Entrada.
3. Completar una máquina y recoger llaves/linternas, ejecutar `inicio reiniciar` y verificar que se restablezcan las estructuras y se liberen las plazas.
4. Repetir la prueba con un spawner en un chunk descargado y volver a cargarlo tras reiniciar.
5. Consultar `inicio estado` y `inicio diagnostico` para comprobar la diferencia de detalle.
