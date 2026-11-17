# Puertas de Nightminigames

**Minecraft 1.21.1 · Fabric · GeckoLib 4.9.3 · Veil Mystrix**

## Colocación

1. Selecciona la región Entrada con la varita existente: `/nightminigames regiones wand entrada`. Pos1 con clic izquierdo y Pos2 con clic derecho.
2. Selecciona la región Salida con `/nightminigames regiones wand salida`.
3. Obtén las puertas: `/nightminigames puertas dar entrada` y `/nightminigames puertas dar salida`.
4. Coloca cada puerta en un lugar despejado; su modelo original ocupa aproximadamente 9 bloques de ancho por 9 de alto. Es necesario tener los bloques cercanos libres para que el volumen de colisión se coloque correctamente.
5. Consulta `/nightminigames puertas estado`.

## Reglas

- Entrada: se abre mientras haya al menos un jugador vivo y no espectador dentro de la región **Entrada** de esa dimensión. Se cierra cuando deja de haber jugadores dentro, siempre que nadie se encuentre atravesando la puerta.
- Salida: se abre en cuanto una máquina de esa dimensión alcance cuatro llaves de cuatro. Permanece abierta mientras al menos una máquina siga completada; se cierra al reiniciar o eliminar la última máquina completada, siempre que no atrape a un jugador.
- Si varias máquinas están completadas, reiniciar una sola no cierra la salida mientras queden otras completadas.
- Los modelos de las dos puertas, animaciones y SFX se obtuvieron del paquete original Flashgame suministrado.
- La configuración de las regiones y el estado de las máquinas completadas se conservan entre reinicios. Las posiciones de las puertas son bloques normales guardados en el mundo.
- No se fuerzan chunks ni se modifican bloques sólidos ajenos. Las puertas no ocupan el espacio de otros bloques y no pueden colocarse si no hay espacio suficiente.

## Comandos

`/nightminigames puertas dar entrada [jugador]`

`/nightminigames puertas dar salida [jugador]`

`/nightminigames puertas estado`

`/nightminigames puertas ayuda`

`/nightminigames maquina resetear` (mirando a la máquina)

Todos los comandos anteriores de Nightminigames siguen disponibles. Los mensajes de regiones en el chat permanecen como estaban.

## Nota técnica

La puerta bloquea una pared de nueve bloques de ancho por nueve de alto mientras está cerrada mediante bloques invisibles propios. Al abrirse, esos bloques se retiran; al destruir la puerta, se limpian. Si un jugador ocupa el volumen de cierre, la puerta espera hasta que esté despejado.
