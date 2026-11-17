# Nightminigames — Administración OP y puntos manuales

## Dependencias

Nightminigames Fabric 1.21.1 utiliza los tipos de la API pública de MystrixPoints 1.2.2 aportada en `integrations/MystrixPoints-1.2.2-fuentes.zip`. En `libs/mystrixpoints-api.jar` hay únicamente las clases públicas de la API para compilar Nightminigames. Ese archivo NO es el mod completo y NO debe instalarse en `mods`.

Para otorgar puntos reales, compila el proyecto MystrixPoints por separado y coloca su JAR completo en `mods` del servidor. En el entorno `runClient`, colócalo también en `run/mods`. MystrixPoints requiere su SQLite incluida en su propia compilación. Nightminigames no modifica su base de datos directamente.

Si MystrixPoints no está instalado, las partidas funcionan, pero `/nightminigames puntos ... confirmar` rechaza la entrega y no inventa saldos.

## Comandos

Todos los comandos de Nightminigames tienen permiso de operador nivel 2 y sus respuestas administrativas aparecen con prefijo `[OP] [Nightminigames]` solo ante operadores. Las notificaciones de juego en HUD no se ocultan.

- `/nightminigames op informe`: información de los siete equipos, cupos, jugadores activos, espectadores, desconectados, clasificación, puntos y estado de integración.
- `/nightminigames puntos estado`: clasificación y estado de pagos para los siete equipos.
- `/nightminigames puntos diagnostico`: detalle completo y destinatarios.
- `/nightminigames puntos equipo rojo`: estado y nombres de los destinatarios del equipo Rojo.
- `/nightminigames puntos equipo rojo 10`: vista previa; **no entrega puntos**.
- `/nightminigames puntos equipo rojo 10 confirmar`: entrega **10 puntos a cada participante de ese equipo que efectivamente clasificó**.

El monto permitido por MystrixPoints para `externalAward` es de 1 a 10000 puntos por participante. Los puntos quedan en la categoría `nightminigames` de MystrixPoints y suman al balance global.

## Seguridad

El sistema guarda la lista de UUID de los participantes elegibles que estaban conectados, vivos y en Supervivencia/Aventura dentro de Salida cuando el equipo clasificó. Desconectados, espectadores y creativos **no** forman parte del pago. Una desconexión posterior no quita a alguien de esa lista. La cantidad por jugador la elige el OP.

Los pagos **nunca son automáticos**. Se exige confirmación y se utiliza el identificador idempotente `qualified:<ronda>:<equipo>` para evitar pagos repetidos incluso después de reiniciar el servidor. El resultado se confirma cuando MystrixPoints responde desde su base de datos. Si MystrixPoints informa que el evento ya existe, no se duplica ningún punto.

La ronda tiene UUID propio que se almacena en `world/data/nightminigames_team_exit.json`. El comando `inicio reiniciar` crea una nueva ronda y vacía el historial de destinatarios y pagos de Nightminigames; no elimina los puntos que MystrixPoints ya otorgó en rondas anteriores.

En clasificaciones registradas antes de actualizar, no existe una lista histórica fiable de jugadores: el pago queda bloqueado hasta una clasificación nueva tras reiniciar la ronda. No se reconstruye desde los nombres actuales para evitar premios a personas incorrectas.

## Ejemplo

Un equipo con 5 integrantes registra a 3 jugadores vivos en la región Salida, uno desconectado y uno en Espectador. El equipo clasifica con 3 destinatarios. Si el OP ejecuta `puntos equipo rojo 15 confirmar`, MystrixPoints añade 15 a cada uno de los 3 participantes, total 45 puntos.

`/nightminigames puntos estado` muestra si la entrega está pendiente o confirmada. La integración es opcional y se ejecuta íntegramente en el servidor.
