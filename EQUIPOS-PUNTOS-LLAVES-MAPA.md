# Nightminigames — equipos, kits y puntos manuales

## Dos kits diferentes

`/nightminigames kit configuracion amarillo` entrega herramientas ADMINISTRATIVAS para montar el recorrido Amarillo (varitas, puerta, máquina y demás). No guarda automáticamente el inventario inicial de sus participantes.

`/nightminigames modulos kit guardar amarillo` guarda los ítems que tenga el OP en los 9 slots de la barra rápida como KIT DEL JUGADOR amarillo.

`kit 0 stacks` significa que no se guardaron ítems para la barra rápida de los participantes. Es válido si quieres comenzar sin kit inicial. Para guardar uno: prepara la barra rápida, ejecuta `/nightminigames modulos kit guardar amarillo`, y consulta `/nightminigames modulos kit ver amarillo`.

## Inicio global con equipos participantes

`/nightminigames equipos autoasignar`
`/nightminigames modulos teleportar todos`
`/nightminigames inicio comenzar`

Se comprueban los destinos y las puertas Entrada solo de los equipos que tienen participantes elegibles distribuidos en esta preparación. No se exige configurar los otros equipos vacíos. Se mantiene el inicio coordinado después de los teletransportes.

`/nightminigames inicio estado` resume la preparación. `/nightminigames inicio diagnostico` muestra el detalle por equipo solo para OP.

## Puntos manuales sin clasificar

`/nightminigames puntos equipo azul 10` muestra los jugadores identificables y el total, sin acreditar nada.
`/nightminigames puntos equipo azul 10 confirmar` acredita la cantidad por jugador a través del mod MystrixPoints, si está instalado.

No exige una plaza de clasificación. Si el equipo clasificó y tiene un snapshot, se utiliza ese snapshot; en caso contrario se utiliza la plantilla identificable actual, incluidos desconectados que tengan UUID en la caché de perfiles. Confirma solo después de revisar los nombres. El pago no se repite para el mismo equipo en la misma ronda. Un reinicio de ronda crea un nuevo identificador de pago; los puntos anteriores no se revierten.

## Llaves y MapaMystrix

`/nightminigames llaves ubicaciones amarillo`
`/nightminigames llaves ubicaciones todos`

El comando es OP. Lista los spawners registrados en el JSON por equipo, con color, estado, dimensión y coordenadas, sin cargar chunks.

Si está instalado el mod `mapmitryx` del ZIP adjunto y su API es compatible, crea o actualiza los waypoints de llaves disponibles bajo un grupo exclusivo del equipo (`nm_amarillo`, etc.). Al ejecutar el comando, sincroniza la membresía online de ese equipo. No aparecen a otros equipos. Los puntos recogidos se retiran del mapa al volver a ejecutar el comando.

Es una integración OPCIONAL y no se incluye el mod MapaMystrix en los dos JAR de Nightminigames. Instálalo por separado en cliente y servidor para visualizar los waypoints. Los marcadores representan el último estado conocido en el JSON; no hacen búsquedas de bloques en mundos sin cargar.

## Mensajes

Los HUD de recogida de llaves y progreso de máquina solo se envían a los integrantes del equipo correspondiente, además de los OP para administración. La clasificación es global. Los datos de HUD previos sin equipo identificado no se importan al historial por equipos; antes de escribir la nueva versión se conservan como `nightminigames_hud_history.json.legacy.bak`.

## Build

`./gradlew.bat --no-daemon clean build` en Windows. Las tareas existentes siguen generando `nightminigames-client-...jar` y `nightminigames-server-...jar` en `libs` si la compilación tiene éxito. No coloques ambos en un mismo servidor. Veil es solo cliente y GeckoLib está requerido por las estructuras compartidas.
