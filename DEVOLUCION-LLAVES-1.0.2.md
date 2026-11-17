# Nightminigames 1.0.2 — devolución de llaves al morir

Actualización del 2 de noviembre de 2026. Conserva los cambios de comandos e inicio de 1.0.1.

Cuando un jugador muere llevando una llave recogida de un spawner con esta versión, la llave se retira del inventario antes de la caída de objetos y se reactiva exactamente el spawner original. No se deja una segunda llave en el suelo. El mismo comportamiento se aplica con keepInventory activado y a la llave que se esté llevando con el cursor del inventario.

Cada llave guarda dimensión, posición, equipo, generación de ronda y un identificador de recogida. La identificación se guarda también en el spawner. Una llave de una recogida o generación anterior no puede reactivar una recogida nueva. Una llave que ya se consumió en una máquina no está en el inventario al morir y no se devuelve.

No se añadió un ticker, barrido periódico ni ticket de chunk permanente. El origen se registra al recoger la llave y se consulta al morir. Si es necesario, el chunk del origen se obtiene con la carga normal de Minecraft en ese momento.

## Límites y actualización

- Las llaves que ya estaban en inventarios antes de 1.0.2 no tienen su origen guardado. Tampoco lo tienen las llaves del kit o dadas por comandos. Se conserva su comportamiento normal; no se adivina un spawner. Para registrar el origen, la llave debe recogerse del spawner después de actualizar.
- Si el spawner original se eliminó o su dimensión ya no existe, se informa a los operadores y se conserva la caída normal de la llave, en lugar de borrarla o recrear un spawner arbitrariamente.
- Una llave cuya recogida o generación ya fue reemplazada se retira sin reactivar el spawner actual.
- El origen viaja con el objeto: si el portador cambia, la devolución corresponde al spawner original, no a la identidad del primer jugador.

Reemplaza el JAR antiguo del mod por el archivo server en el servidor y el archivo client en los clientes. No instales ambas variantes en el mismo lado. No hacen falta nuevos comandos. Se mantienen las dependencias de la versión anterior.

## Validación

Compilación del proyecto y prueba de servidor aislado con jugadores sintéticos. Se ejecutó la ruta real ServerPlayer.die para muerte normal, keepInventory y cursor. Resultado: 18 comprobaciones aprobadas, incluyendo origen persistido en el objeto y spawner, no afectar otro spawner del mismo color, no generar una llave caída adicional, conservar la caída de objetos normales, claves antiguas y origen eliminado. No hubo jugadores reales conectados ni validación visual de cliente.
