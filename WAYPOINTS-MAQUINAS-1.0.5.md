# Nightminigames 1.0.5 — waypoint de máquina para todos los equipos

Los waypoints de máquinas están DESACTIVADOS por defecto. Solo un operador con nivel 2 o superior, o la consola del servidor, puede cambiar la opción.

/nightminigames waypoints maquina activar

Habilita los waypoints de máquinas para los siete equipos con un solo comando. Cada equipo recibe únicamente el marcador de sus propias máquinas vinculadas. La máquina debe estar colocada y vinculada mediante el kit de configuración. Los marcadores se muestran durante la partida de ese equipo y con las luces encendidas; se ocultan con luces apagadas, equipo detenido o clasificado y al finalizar la ronda.

/nightminigames waypoints maquina desactivar

Desactiva y retira todos los marcadores de máquinas. No desactiva los waypoints de llaves.

/nightminigames waypoints maquina estado

Informa para cada uno de los siete equipos si la opción está habilitada, cuántas máquinas tiene registradas y el estado de visibilidad. Si falta la máquina o su vinculación, lo indica.

La opción del OP se guarda por mundo y se conserva al reiniciar rondas y servidor hasta que el OP la cambie. Reiniciar una ronda retira los marcadores de juego hasta el siguiente inicio.

## Evaluación previa

Tras habilitar la opción, /nightminigames llaves vista todos también muestra las máquinas de los siete equipos, con marcadores personales del operador. /nightminigames llaves vista rojo filtra llaves y máquinas habilitadas del equipo rojo. La evaluación funciona antes de iniciar, con luces apagadas, y se retira al comenzar. Los jugadores normales no reciben los marcadores de evaluación.

Se utiliza el icono factory incluido en MapaMystrix para identificar la máquina, con tamaño base 32. El mapa conserva su configuración de escala y zoom. El índice persistente de máquinas permite sincronizar sus ubicaciones sin forzar la carga de todos los chunks.

## Instalación

Reemplaza los JAR anteriores de Nightminigames por la variante server 1.0.5 en el servidor y la variante client 1.0.5 en clientes. MapaMystrix 3.1.3 y las dependencias de las versiones anteriores se mantienen. Se incluyen las funciones de llaves y evaluación de 1.0.4.

## Validación

53 comprobaciones aprobadas en prueba aislada con MapaMystrix 3.1.3 y jugadores sintéticos: valor desactivado por defecto; rechazo a jugador sin OP; activación conjunta de los siete equipos; evaluación personal de las siete máquinas; visibilidad solo del equipo propio; desactivación sin afectar llaves; eliminación y reposición de máquina; luces; recarga de estado; retirada al completar clasificación; y reinicio conservando la preferencia del OP. También se ejecuta la regresión de llaves y evaluación.

No se comprobó la apariencia con un cliente Xaero conectado ni una partida multijugador real. No se modificaron mundos del usuario. Gradle requiere el lanzador Java 25, manteniendo las clases y el servidor de Minecraft en Java 21.
