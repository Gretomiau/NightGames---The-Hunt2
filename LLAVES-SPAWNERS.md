# Nightminigames — Cuatro spawners de llaves

## Varitas (solo OP)

- `/nightminigames llaves wand azul`
- `/nightminigames llaves wand amarilla`
- `/nightminigames llaves wand roja`
- `/nightminigames llaves wand verde`

Cada comando admite `[jugador]` como argumento opcional. Las varitas también están en la pestaña creativa Nightminigames.

## Configuración del spawner

- Clic derecho sobre un bloque con una varita: coloca un spawner del color de la varita en la cara seleccionada.
- Agacharse + clic derecho con una varita sobre un spawner: elimina ese spawner.
- Clic derecho con una varita sobre un spawner recogido: lo reactiva.
- `/nightminigames llaves spawner reactivar`: reactiva el spawner que estés mirando.
- `/nightminigames llaves spawner eliminar`: elimina el spawner que estés mirando.
- `/nightminigames llaves ayuda`: muestra los comandos de configuración.

Los spawners se guardan como bloques del mundo, incluyendo su estado de recogida, y persisten tras reinicios y en distintas dimensiones.

## Recogida por jugadores

El holograma muestra la llave original de GeckoLib, flotante y girando. No es un ItemEntity recogible automáticamente.

- Apunta al holograma desde un máximo aproximado de 5 bloques.
- Mantén pulsado clic derecho sin dejar de apuntar durante 6 segundos (120 ticks a 20 TPS).
- El panel sci-fi muestra el progreso de 0 a 100 % y adapta su escala a la resolución.
- Soltar clic, retirar la mira, abrir una pantalla o alejarse cancela el progreso.
- Al completar, el servidor entrega una llave del color correspondiente y marca ese spawner como recogido.
- El holograma desaparece cuando se recoge; un OP puede reactivarlo con varita o comando.

## Dar llaves directamente

- `/nightminigames llaves dar azul [jugador]`
- `/nightminigames llaves dar amarilla [jugador]`
- `/nightminigames llaves dar roja [jugador]`
- `/nightminigames llaves dar verde [jugador]`

## Requisitos

Minecraft Fabric 1.21.1, Java 21, Fabric API, GeckoLib 4.9.3 y Veil Mystrix del ZIP. El código fuente Java no contiene comentarios.

## Compilación

`gradlew.bat --no-daemon clean build`

`gradlew.bat --no-daemon runClient`

La compilación y el renderizado necesitan prueba final en IntelliJ IDEA. No está validado el arranque completo de Minecraft en este entorno.
