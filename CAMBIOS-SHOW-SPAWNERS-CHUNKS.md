# Nightminigames — Puertas, visualización y spawners

## Rendimiento de puertas

El gestor de puertas ya no recorre las puertas cada vez que Minecraft carga un chunk. Solo registra o descarga las puertas mediante el ciclo de vida de sus entidades de bloque y procesa hasta 12 posiciones de colisión por tick.

No se introducen tickets ni cargas forzadas de chunks. Las puertas y máquinas situadas en chunks descargados quedan inactivas hasta que Minecraft las carga normalmente.

## Visualización de configuración

- `/nightminigames show`: alterna la visualización administrativa.
- `/nightminigames show activar`: activa la visualización.
- `/nightminigames show desactivar`: la desactiva.
- `/nightminigames show estado`: consulta el estado.

Las regiones se dibujan en el mundo sin necesidad de sostener una varita. En creativo, los spawners muestran siempre un icono flotante; con `show` activo también muestran su nombre, las máquinas y las puertas cargadas. El comando requiere OP. Ningún sistema carga chunks para dibujar configuraciones.

## Spawner de linterna

- `/nightminigames linterna spawner wand`: entrega la varita para colocar spawners.
- `/nightminigames linterna spawner wand <jugador>`: entrega la varita a otro jugador.
- `/nightminigames linterna spawner reactivar`: reactiva el punto al que miras.
- `/nightminigames linterna spawner eliminar`: elimina el punto al que miras.

Con la varita, clic derecho sobre un bloque coloca el spawner. Clic derecho sobre el spawner agotado lo reactiva; agachado y clic derecho lo elimina.

El spawner conserva el modelo 3D de Flashgame, ofrece una sola linterna por activación y guarda en el mundo si ya fue recogida. Los jugadores la obtienen con clic derecho sobre el punto. No depende del sistema de equipos de Flashgame ni produce objetos físicos adicionales que queden cargados continuamente.

## Iconos administrativos

Los spawners de las cuatro llaves y los spawners de linterna muestran iconos flotantes exclusivamente a jugadores en modo Creativo. Los hologramas normales permanecen visibles para los participantes.

Los demás sistemas del proyecto mantienen su comportamiento.
