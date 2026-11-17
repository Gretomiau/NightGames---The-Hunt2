# Nightminigames 1.0.3 — llaves en MapaMystrix

Actualización del 8 de noviembre de 2026 para Minecraft Fabric 1.21.1.
Compatible y probado con el JAR existente MapaMystrix 3.1.3 de E:\2026-08-22\MapaMystrix-puzzle-team-waypoint-api.

## Instalación

1. Sustituye el JAR de Nightminigames en el servidor por nightminigames-server-1.0.3+1.21.1.jar y en los clientes por nightminigames-client-1.0.3+1.21.1.jar.
2. Mantén MapaMystrix 3.1.3, Fabric Language Kotlin y sus dependencias habituales. El mapa se utiliza mediante su API existente; esta entrega no sustituye su JAR.
3. En el primer arranque conjunto se copian automáticamente los cuatro iconos a la carpeta efectiva de imágenes de MapaMystrix, normalmente config/mapmitryx/waypoint-images/nightminigames/key_blue.png, key_yellow.png, key_red.png y key_green.png. Si el operador ya personalizó esos archivos, se conservan.
4. MapaMystrix sincroniza los PNG con cada cliente autorizado. No hay que copiarlos manualmente a cada cliente.

También se añadieron los PNG al directorio server-config-example/mapmitryx/waypoint-images/nightminigames del proyecto MapaMystrix que indicaste.

## Comprobación de las cuatro llaves

/nightminigames llaves comprobar
/nightminigames llaves comprobar todos
/nightminigames llaves comprobar rojo

El comando revisa los colores azul, amarillo, rojo y verde colocados como spawners de cada equipo. Informa COMPLETO 4/4 o qué colores faltan, cuántos spawners tiene cada color y cuántas llaves están disponibles. Tener varios spawners de un color no sustituye otro color faltante. Las llaves ya recogidas siguen contando como spawners colocados, pero dejan de estar disponibles. La comprobación usa el índice persistente de ubicaciones; no carga todos los chunks ni comprueba inventarios de jugadores.

## Inicio y luces

/nightminigames inicio comenzar
/nightminigames inicio comenzar rojo

Los waypoints se activan automáticamente para los equipos iniciados. Cada equipo recibe únicamente sus propias llaves pendientes, con el PNG del color correspondiente. Los jugadores sin equipo no reciben esos marcadores.

/nightminigames oscuridad desactivar

Enciende las luces y muestra los waypoints de llaves disponibles de los equipos iniciados.

/nightminigames oscuridad activar

Apaga las luces y oculta los waypoints. Volver a encenderlas muestra las llaves que sigan disponibles.

Al recoger una llave se retira su marcador; si vuelve al spawner por una muerte, reaparece cuando corresponda por luces y ronda. Los comandos de ubicaciones no permiten saltarse esas condiciones. La sincronización agrupa los cambios por eventos y solo actualiza los marcadores modificados; no reconstruye continuamente todos los waypoints ni añade tickets permanentes de chunks.

## Clasificación y fin

/nightminigames clasificacion cupos 3

Ejemplo: termina al clasificar tres equipos. Se usa el cupo real configurado del juego, no una cantidad nueva dentro del mapa. Un equipo que ya clasificó deja de recibir sus marcadores; los demás equipos iniciados continúan. Cuando se completa el cupo se retiran todos los waypoints de llaves de Nightminigames. Encender las luces después no los reactiva.

/nightminigames inicio reiniciar

Prepara otra ronda. Los marcadores permanecen ocultos hasta que vuelvas a iniciar. inicio detener también retira los marcadores afectados. El estado de equipos iniciados se guarda ligado al identificador de ronda en data/nightminigames_waypoints.json.

## Iconos

Cuatro PNG RGBA de 32x32 con fondo transparente, creados como símbolos vectoriales de llave y exportados con resvg. Se incluyen fuentes SVG y vista previa. No son capturas de los modelos 3D. La escala visual final del mapa sigue su configuración de zoom y tamaño de iconos.

## Validación

27 comprobaciones aprobadas en un servidor Fabric aislado con el JAR real MapaMystrix 3.1.3, Fabric Language Kotlin 1.13.12+kotlin.2.4.0 y jugadores sintéticos. Se verificaron cuatro colores frente a color faltante; instalación y resolución de los cuatro PNG; privacidad por equipo; inicio/luces; recogida y devolución; actualización sin reescribir marcadores iguales; retirada al abandonar equipo; recarga del estado activo; equipo clasificado; ruta terminal al completar cupo; y reinicio sin activación anticipada.

El estado de clasificación se preparó como fixture y se invocó la ruta real de fin de ronda; no fue una partida completa con jugadores reales. La devolución de llave se invocó desde el sistema de devolución ya probado en 1.0.2. No se verificó visualmente el mapa en un cliente con Xaero ni se realizó una conexión multijugador real. No se modificaron mundos del usuario.

El proyecto fuente del mapa carece de gradle.properties, lanzadores gradlew y fabric.mod.json en las rutas esperadas; no se reconstruyeron esos archivos ni se recompiló MapaMystrix. Se verificó la integración contra su JAR existente. El código de ciclo de juego reside en el puente de Nightminigames y los iconos se distribuyen por la API de assets del mapa.
