# Nightminigames — Persistencia, dimensiones y linternas multijugador

Versión objetivo: Fabric 1.21.1, siete equipos, cliente con Veil Mystrix y GeckoLib.

## Datos por mundo

Todos los archivos de configuración administrados por el servidor se almacenan en el directorio del mundo.

| Archivo | Contenido |
|---|---|
| `data/nightminigames_modulos.json` | Destinos por equipo, dimensiones, orientaciones, kits y entregas realizadas |
| `data/nightminigames_team_regions.json` | Pos1/Pos2 para Entrada y Salida, equipo y dimensión |
| `data/nightminigames_team_cupos.json` | Cupos de jugadores por equipo |
| `data/nightminigames_team_exit.json` | Equipos clasificados, cupos, miembros y recompensas confirmadas |
| `data/nightminigames_completed_machines.json` | Máquina completada por dimensión y equipo y cierre de Salida |
| `data/nightminigames_round_generation.json` | Versión de la ronda y reinicio de spawners |
| `data/nightminigames_hud_history.json` | Historial permanente de recogidas y progreso |
| `data/nightminigames_classification_hud.json` | Historial y límite de clasificaciones |
| `data/nightminigames_environment.json` | Oscuridad, música y volumen, migrado desde el archivo global si existe |
| `data/nightminigames_flashlight.json` | Perfil de iluminación Veil: alcance, intensidad, ángulos y tamaño de luces |
| `data/nightminigames_admin_show.json` | Estado y filtro de `/nightminigames show` por OP |
| `data/nightminigames_structures.json` | Índice consultable de estructuras conocidas y su último estado, dimensiones y plantilla de equipos |

El índice `nightminigames_structures.json` guarda dimensiones, coordenadas, tipo, equipo, estado de apertura, progreso y recogida de puertas, máquinas, spawners de llaves y linternas. Registra entidades de bloque cargadas, actualiza un número limitado por tick y guarda cambios cada cinco segundos aproximadamente y durante el cierre normal. Crea `nightminigames_structures.json.bak` antes de reemplazar el índice existente. Si el archivo original está dañado, no se sobrescribe.

**Importante:** la geometría y la persistencia real de bloques de Minecraft, las entidades de bloque y los inventarios de jugadores siguen utilizando los archivos de mundo (`region`, `entities`, `playerdata` y NBT), porque el formato de guardado nativo es necesario para restaurar físicamente esas estructuras. El JSON es una copia consultable y un índice administrativo, no reemplaza los chunks ni constituye por sí solo una copia completa del mundo. Los equipos de Minecraft se conservan en el scoreboard vanilla y se reflejan como lista de nombres en el índice JSON. La economía de MystrixPoints se guarda en MystrixPoints, no se modifica aquí.

## Comandos OP

```
/nightminigames persistencia estado
/nightminigames persistencia guardar
/nightminigames persistencia dimensiones
/nightminigames persistencia linterna recargar
```

`persistencia guardar` fuerza el índice JSON; los demás sistemas guardan sus propios archivos cuando cambia su configuración. El comando `linterna recargar` lee el perfil `nightminigames_flashlight.json` y lo sincroniza con los clientes conectados.

## Linternas vistas por otros jugadores

El estado de encendido y la dimensión se envían desde el servidor. Cada cliente renderiza con Veil las linternas de participantes cercanos situados en **la misma dimensión**. El haz ajeno se orienta según la dirección de mirada interpolada del jugador y nace cerca de su mano, no desde la cámara de quien observa. El renderizador limita el número de emisores próximos para controlar FPS en partidas grandes. Los estados se actualizan al cambiar de dimensión y al entrar o salir del servidor. La batería y el encendido siguen validados por el servidor.

## Dimensiones adicionales

Las regiones, módulos, spawners y puertas se identifican por el ID completo de dimensión, por ejemplo `minecraft:overworld` o `mi_mundo:arena`.

`/nightminigames persistencia dimensiones` enumera los mundos actualmente cargados como `ServerLevel` en el mismo servidor Fabric. El sistema no fuerza chunks nuevos ni se integra con la API de plugins Bukkit/Paper. Los mods de varios mundos de Fabric que registren dimensiones como `ServerLevel` pueden usar las mismas herramientas. Para teletransportes, el mundo y chunk de destino deben estar cargados.

## Publicación

Ejecuta en IntelliJ con Java 21:

```
.\gradlew.bat --no-daemon clean build
```

La tarea Gradle del proyecto deja los JAR de `nightminigames-client-1.0.0+1.21.1.jar` y `nightminigames-server-1.0.0+1.21.1.jar` en `libs`. Instala solo el correspondiente a cada lado. El cliente requiere Veil Mystrix, Fabric API y GeckoLib; el servidor requiere Fabric API y GeckoLib, sin Veil. Para puntos reales instala el mod MystrixPoints correspondiente.

Antes de sustituir el proyecto o de usar nuevos archivos JSON, realiza una copia de seguridad de toda la carpeta del mundo. No basta con copiar la carpeta `data`.
