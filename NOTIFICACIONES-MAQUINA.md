# Notificaciones HUD — Máquina de cuatro llaves

La información de la máquina aparece como notificaciones temporales y no como mensajes de progreso del chat.

Al colocar una llave, todos los jugadores conectados reciben una notificación con el nombre del jugador, la llave insertada, el número de cerraduras completadas y las que faltan. Cuando se coloca la cuarta llave, aparece el estado **MÁQUINA COMPLETADA**.

Las notificaciones aparecen en la parte superior derecha con colores sci-fi, transición de entrada/salida y adaptación a la escala de GUI. Se reproducen secuencialmente si varias personas insertan llaves simultáneamente. La duración de cada notificación es de aproximadamente 4,4 segundos.

El comando `/nightminigames maquina estado` muestra únicamente al operador una notificación temporal con el nombre de quien colocó cada llave y las que siguen pendientes.

El comando `/nightminigames maquina resetear` emite una notificación global de reinicio. Insertar una llave repetida muestra una advertencia únicamente al jugador que lo intenta.

## Sonidos incluidos

| Archivo de origen | Evento | Alcance |
|---|---|---|
| `mightuser-key-lock-insertion-mechanism-sfx-hd-269470.mp3` | Inserción | Cercanos a la máquina |
| `mixkit-gear-metallic-lock-sound-2858.wav` | Bloqueo confirmado | Cercanos, con retardo breve |
| `mixkit-sci-fi-click-900.wav` | Aviso de progreso | Global |
| `mixkit-classic-alarm-995.wav` | Máquina completada | Global |

Los cuatro archivos se convierten a OGG Vorbis para su reproducción dentro de Minecraft.

## Prueba

Ejecuta `./gradlew.bat --no-daemon clean runClient` en Windows. Coloca una máquina, inserta llaves distintas y consulta su estado. Para verificar la notificación global, prueba con dos clientes conectados.

No se ha realizado una compilación Gradle ni una prueba gráfica en Minecraft en este entorno.
