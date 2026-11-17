# Máquina de llaves

Máquina de Flashgame adaptada a Nightminigames Fabric 1.21.1.

- Estructura de 3 bloques de ancho y 2 de alto; bloque maestro en el centro de la base.
- Utiliza el modelo y la textura originales de Flashgame con GeckoLib.
- Cuenta con cuatro cerraduras para las llaves azul, amarilla, roja y verde.
- Insertar una llave con clic derecho la consume en supervivencia, y actualiza el indicador holográfico de esa cerradura.
- El progreso se conserva en el estado del bloque (4 bits, 0–15), y persiste tras reinicios del mundo.
- Acepta llaves que vienen de los spawners y las obtenidas mediante comandos.
- Cuando se insertan las cuatro se confirma la finalización de la máquina. No activa puertas, porque no hay un sistema de puertas enlazado en este proyecto.
- Si la estructura completa no cabe en el lugar elegido, no permite colocarla.
- Al romper una parte, se elimina la estructura y el bloque principal deja caer la máquina.
- Se reconstruyen piezas faltantes al cargar un bloque principal, sin forzar carga de chunks ni sustituir bloques sólidos.

## Comandos OP

- `/nightminigames maquina dar`
- `/nightminigames maquina dar <jugador>`
- `/nightminigames maquina estado` (mirando la máquina)
- `/nightminigames maquina resetear` (mirando la máquina)
- `/nightminigames maquina eliminar` (mirando la máquina)
- `/nightminigames maquina ayuda`

También puedes obtener el ítem mediante `/give @s nightminigames:machine`.

## Notas

La extracción de llaves durante 6 segundos, la notificación global líquida y la linterna permanecen sin cambios. La máquina no necesita un timer por tick para sus cerraduras.
