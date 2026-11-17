# Nightminigames — configuración independiente de siete equipos

Los siete equipos son `rojo`, `azul`, `verde`, `amarillo`, `naranja`, `morado` y `rosa`. Cada uno dispone de sus propios puntos, regiones, puertas, máquina y spawners. Los kits de jugadores y sus cupos continúan gestionándose por módulo.

## Preparación

1. `/nightminigames equipos preparar`
2. `/nightminigames equipos cupos todos 20`
3. `/nightminigames kit configuracion rojo`
4. Repite el tercer comando para `azul`, `verde`, `amarillo`, `naranja`, `morado` y `rosa`.
5. Configura un recorrido por cada equipo utilizando los elementos de **su** kit.
6. `/nightminigames equipos autoasignar`
7. `/nightminigames modulos teleportar todos`
8. `/nightminigames inicio comenzar` abre simultáneamente las siete puertas Entrada si todos los participantes llegaron correctamente. Si aún está en curso el teletransporte global, el inicio queda pendiente hasta terminar.

## Contenido de cada kit administrativo

El kit de un color contiene las cuatro varitas de llaves, varita de spawner de linterna, varita de módulo, varitas de región Entrada/Salida, una máquina, ambas puertas, las cuatro llaves y una linterna. Los objetos de administración llevan un identificador del equipo en sus datos del ítem y un nombre del color correspondiente.

### Ejemplo de configuración del equipo Azul

- `/nightminigames kit configuracion azul`
- Con la varita **Entrada Azul**: clic izquierdo Pos1, clic derecho Pos2.
- Con la varita **Salida Azul**: selecciona Pos1 y Pos2 del recorrido Azul.
- Con el stick de módulos Azul: clic derecho para guardar su punto de llegada.
- Coloca la puerta Entrada Azul y la puerta Salida Azul. La puerta Entrada consulta solo su región; la Salida consulta solo las máquinas completadas del Azul.
- Coloca la máquina Azul. Las llaves Azul de equipo (azul, roja, amarilla, verde) se obtienen de sus spawners correspondientes. El color del equipo y el color de la llave son cosas distintas.
- `/nightminigames show azul` muestra las regiones y etiquetas administrativas del equipo Azul únicamente.
- `/nightminigames show activar` muestra todas las configuraciones cargadas. `/nightminigames show desactivar` las oculta.
- `/nightminigames inicio comenzar azul` abre la puerta Entrada Azul.

## Administración

- `/nightminigames regiones estado`: estado de Entrada y Salida de los 7 equipos.
- `/nightminigames regiones limpiar entrada azul`: borra solo Entrada del Azul.
- `/nightminigames puertas estado`: puertas cargadas por equipo.
- `/nightminigames modulos estado`: destinos, cupos y kits de jugadores.
- `/nightminigames modulos kit guardar azul`: guarda los objetos de la barra rápida del OP como kit **de participantes** Azul; es distinto al kit de configuración administrativa.
- `/nightminigames inicio detener azul`: desactiva solo la apertura inicial del Azul.

## Guardado y compatibilidad

Las 14 selecciones se guardan en `world/data/nightminigames_team_regions.json`. Las regiones de la implementación anterior (`world/data/nightminigames_regions.json`) **no se borran ni se copian automáticamente** porque eran compartidas; configura las nuevas selecciones específicas por equipo. Los puntos y kits de módulos que ya tenían color se conservan en `world/data/nightminigames_modulos.json`.

Los bloques que se colocaron antes de esta versión pueden carecer de propietario. Para un recorrido independiente, usa las versiones incluidas en los kits por color. Haz copia de seguridad del mundo antes de cambiar los bloques existentes.

Los renderizados de selección y las etiquetas se procesan en cliente, sin forzar chunks; el servidor valida la pertenencia, las recogidas y las transiciones de puertas. Los operadores pueden usar los objetos de prueba y los participantes deben estar asignados al equipo correspondiente. Los shaders, sonidos y widgets existentes se conservan.

El comando global `inicio comenzar` no abre parcialmente los recorridos. La reunión previa no es necesaria y se retiró de los comandos disponibles.
