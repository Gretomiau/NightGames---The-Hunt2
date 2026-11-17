# Nightminigames — inicio global de los siete equipos

Minecraft Fabric 1.21.1. Los siete equipos mantienen configuraciones independientes. La reunión previa se retiró del flujo principal.

## Configuración previa de administración

1. `/nightminigames equipos preparar` crea los siete equipos si todavía no existen.
2. `/nightminigames equipos cupos todos 20` configura hasta 140 plazas en total.
3. `/nightminigames kit configuracion rojo` y los otros seis colores entregan herramientas específicas de cada equipo.
4. Marca las regiones Entrada/Salida y coloca las puertas Entrada/Salida de cada equipo.
5. Configura el punto de módulo de cada equipo con su stick correspondiente. `/nightminigames modulos punto estado` permite revisar los destinos.
6. Guarda los kits de los participantes con `/nightminigames modulos kit guardar <equipo>`.
7. `/nightminigames equipos autoasignar` distribuye aleatoriamente a los participantes en Supervivencia/Aventura según sus cupos.

## Dos comandos globales

```mcfunction
/nightminigames modulos teleportar todos
/nightminigames inicio comenzar
```

El primero comprueba que todos los participantes elegibles estén asignados a uno de los siete equipos y que los destinos de los equipos ocupados estén configurados y sus chunks cargados. Si falta un jugador por asignar o un destino necesario, cancela toda la operación sin teletransportar a nadie. Después envía hasta cuatro participantes por tick y entrega sus kits de módulo una sola vez por ronda.

El segundo abre las siete puertas Entrada al mismo tiempo, una por equipo, tras verificar que todas las regiones y puertas Entrada estén configuradas y cargadas. Si todavía se están ejecutando los teletransportes globales, programa el inicio y abre las siete puertas automáticamente al finalizar correctamente. Si algún jugador no llega o la configuración no está completa, el comienzo se cancela. No existe inicio parcial desde este comando global.

No necesitas ejecutar `/nightminigames inicio reunir`: el comando se retiró. El inicio no requiere que los siete módulos estén en la dimensión del OP; cada puerta utiliza la dimensión de la región Entrada de su propio equipo.

## Estados y administración

- `/nightminigames inicio estado` informa si el teletransporte global está en curso, si el inicio quedó pendiente, y si las siete puertas están preparadas.
- `/nightminigames modulos estado` incluye los cupos, puntos y kits de los módulos.
- `/nightminigames inicio detener` retira las aperturas iniciales de los siete equipos, sin borrar sus máquinas.
- `/nightminigames modulos kit reiniciar` reinicia el registro de kits ya entregados para la siguiente ronda; no elimina objetos del inventario.
- Los comandos individuales de un equipo continúan disponibles para intervenciones administrativas excepcionales.

Solo se distribuyen jugadores conectados en Supervivencia o Aventura. Los OP en Creativo no se teletransportan. Los puntos deben estar previamente cargados: esta implementación no fuerza la carga de chunks. Si un jugador se desconecta o cambia de equipo durante la distribución, el comienzo global no se autoriza y el OP deberá corregir el problema y repetir el teletransporte.

Los datos de módulos y kits permanecen en `world/data/nightminigames_modulos.json`. Los siete recorridos y los estados de puerta/máquina mantienen sus archivos de guardado actuales. El estado de una distribución activa no se restaura tras un reinicio: debe volverse a ejecutar el teletransporte global.
