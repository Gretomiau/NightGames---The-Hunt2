# Nightminigames 1.0.8 — terror flotante y ticker global

## Flotantes

- /nightminigames terror poner flotante
- /nightminigames terror poner flotante ~ ~5 ~20
- /nightminigames terror quitar flotante
- /nightminigames terror quitar todos
- /nightminigames terror estado

Sin coordenadas, el punto señalado se eleva 2,5 bloques. Con coordenadas se usa exactamente esa base. La figura oscila ±0,12 bloques. Comparte el vestido y cuerpo 3D de la silueta; no requiere entidades.

Las apariciones automáticas ahora pueden elegir la variante flotante (una opción entre tres al comenzar cada búsqueda). Se busca espacio libre entre 1 y 6 bloques por encima de la altura del jugador, sin exigir suelo debajo, en zonas cargadas y con línea de visión. Las otras variantes mantienen la colocación sobre suelo. Se reserva espacio adicional para la oscilación y se respetan los límites verticales del mundo.

## Ticker exclusivo del terror

Un registro global END_CLIENT_TICK por cliente reúne toda la lógica de terror. Comprueba el cambio de mundo en cada tick para limpiar el estado; ejecuta la lógica de apariciones y linterna cada 4 ticks (5 pasadas por segundo a 20 ticks/s). Se acumulan los ticks transcurridos para conservar duración y esperas. No hay hilos ni temporizadores independientes por figura. El renderizado sigue por fotograma, con geometría calculada y almacenada una vez.

Presupuesto compartido máximo: 64 rayos de visibilidad por pasada para linternas y colocaciones. Hasta 16 linternas cercanas se consideran una sola vez por pasada. Durante las esperas sin figuras activas se omite esa selección. La búsqueda de terreno conserva un máximo de 8 columnas por intento y reintentos cada 5 segundos; solo consulta zonas cargadas. Si falta presupuesto para validar completamente una nueva posición, se pospone la aparición.

El servidor no necesita recorrer todas las apariciones: son presentaciones privadas en cada cliente. El control OP y las órdenes manuales continúan en el servidor. Ningún ticker de llaves, máquinas o del resto del mod se modifica.

## Comportamiento conservado

Apariciones automáticas solo durante la oscuridad sincronizada del mod. Una por jugador. Sin linterna: 18–28 bloques. Con linterna encendida: alcance + 8 a alcance + 18, con mínimo de 24. Perfil predeterminado: 56–66 bloques. La linterna puede eliminar las figuras si alcanza cabeza, torso o pies sin una pared entre ambos. El cuerpo flotante y su prueba de linterna usan la misma oscilación. No se sube brillo ni se añaden luces.

- /nightminigames terror automatico activar
- /nightminigames terror automatico desactivar
- /nightminigames terror automatico estado

El interruptor automático es por sesión del servidor y está encendido por defecto al arrancar. Mantiene los comandos manuales silueta, presencia, movimiento, sombra y cabeza; cada OP ve sus propias pruebas.

## Validación

BUILD SUCCESSFUL, objetivo Java 21; metadatos y separación de JAR cliente/servidor comprobados. Prueba de cadencia: 200 ticks generan 50 pasadas y preservan 200 ticks transcurridos; reinicio de cadencia comprobado. Sin pruebas visuales, de multijugador ni medición FPS/TPS de esta versión. La limitación de trabajo reduce comprobaciones, pero no constituye una garantía medida de ausencia de lag.

Actualizar el par de JAR y reiniciar Minecraft/servidor. El cliente abierto previamente no contiene estos últimos cambios.
