# Nightminigames 1.0.4 — evaluación previa de llaves

Para revisar la colocación antes de iniciar, entra con permisos de operador y ejecuta:

/nightminigames llaves vista rojo

Muestra cada spawner de llave del equipo rojo como waypoint personal en tu mapa. Admite cualquiera de los siete equipos.

/nightminigames llaves vista todos

Muestra todas las llaves colocadas de los siete equipos. Cada marcador indica EVAL, equipo, color y estado DISPONIBLE o RECOGIDA. También incluye spawners recogidos y funciona con las luces apagadas para permitir la evaluación.

/nightminigames llaves vista ocultar

Retira tu vista de evaluación. Los comandos se ejecutan como jugador operador; la consola no tiene una vista personal de mapa.

Los marcadores de evaluación son personales y solo los recibe el operador que los solicitó. Al iniciar la partida se retiran automáticamente; la evaluación no puede abrirse mientras haya equipos iniciados. A partir de ahí se aplican las reglas normales de luces, llaves disponibles y clasificación. Al desconectarte, detener o reiniciar se limpia la vista. No se conserva una sesión de evaluación tras reiniciar el servidor.

Para el resumen de colores colocados sigue disponible:
/nightminigames llaves comprobar [equipo]

Instalación: sustituye los JAR anteriores de Nightminigames por la variante server en servidor y client en clientes. Compatible con MapaMystrix 3.1.3 existente. Los iconos y demás funciones de 1.0.3 se incluyen en esta versión.

Validación: 35 comprobaciones aprobadas en prueba aislada con MapaMystrix real y jugadores sintéticos, incluidos permisos personales, inclusión de recogidas en oscuridad, selección de equipo/todos, ocultación y retirada al comenzar, además de la regresión del ciclo normal. No se comprobó visualmente en un cliente Xaero conectado.
