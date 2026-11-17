# Nightminigames — guardado automático por eventos

Esta versión reemplaza el recorrido periódico y la escritura programada cada 100 ticks del índice `world/data/nightminigames_structures.json`.

## Cuándo se detectan los cambios

- Al modificar el propietario o el estado de una puerta, máquina o spawner, su entidad de bloque marca el registro como modificado.
- Al recoger o reactivar una llave o linterna, se marca el cambio del estado del bloque.
- Al cargar o descargar una entidad de bloque registrada, se actualiza la información disponible sin cargar otros chunks.
- Al cambiar los integrantes de los equipos mediante los comandos del mod, se marca el índice como modificado.

Los cambios se consolidan en memoria. No se recorre la lista de estructuras cada tick y no se escribe periódicamente a disco por un temporizador propio.

## Cuándo se escribe el JSON

- Después del guardado normal del mundo por Minecraft, incluidos los guardados solicitados con `/save-all`.
- Al apagar el servidor correctamente, si existen cambios pendientes.
- No se reescribe si la información es idéntica a la última versión guardada.

Se conserva la escritura mediante archivo temporal, el respaldo `.bak` y el reemplazo atómico cuando el sistema de archivos lo permite.

## Datos existentes

El archivo JSON mantiene su estructura, sus dimensiones y las referencias a equipos, puertas, máquinas y spawners. Se conservan los demás archivos JSON del mod y la persistencia nativa de Minecraft de los bloques, inventarios y scoreboard.

## Importante

Este esquema no elimina la necesidad de copias de seguridad. Un cierre forzado puede perder modificaciones realizadas después del último guardado normal del mundo. Los archivos JSON independientes del mod conservan sus mecanismos de guardado existentes.

## Comprobación

1. Iniciar Minecraft y modificar un spawner o máquina.
2. Ejecutar `/save-all` o cerrar el servidor normalmente.
3. Verificar la modificación en `world/data/nightminigames_structures.json`.
4. Confirmar que no se produce una escritura repetitiva del índice durante el juego sin cambios.

El comando `/nightminigames persistencia estado` muestra el estado del índice, sin forzar una escritura.
