# Nightminigames 1.0.1 — comandos e inicio simplificados

Cambios aplicados al proyecto Fabric 1.21.1 el 1 de noviembre de 2026.

## Inicio

- /nightminigames inicio comprobar: revisa los siete equipos y dice exactamente qué falta.
- /nightminigames inicio comprobar rojo: revisa solo rojo (también admite los otros seis colores).
- /nightminigames inicio comenzar: valida y distribuye automáticamente a los participantes de los equipos activos; entrega los kits de jugadores cuando corresponda y abre las entradas al terminar.
- /nightminigames inicio comenzar rojo: inicia únicamente rojo.
- /nightminigames inicio detener [equipo]: detiene la apertura inicial.
- /nightminigames inicio reiniciar: reinicia la ronda conservando equipos, regiones, destinos y kits.

Se admiten equipos incompletos y un solo equipo. Los jugadores sin equipo, los creativos, los espectadores y los equipos sin participantes conectados no bloquean a los demás. Los participantes del minijuego siguen siendo jugadores en Supervivencia o Aventura asignados a uno de sus siete equipos.

No hace falta ejecutar modulos teleportar todos antes de comenzar. Ese comando sigue disponible como teletransporte manual. Los destinos ya no tienen que estar cargados previamente: el servidor obtiene el chunk necesario al teletransportar. No se añaden chunks forzados permanentes.

Para iniciar se necesita un destino válido, una región de entrada completa y una puerta de entrada del equipo en su dimensión. El comando de comprobación también avisa de lo que falta para el recorrido completo: región de salida, puerta de salida, máquina y spawners de los cuatro colores. Esos avisos del recorrido no impiden abrir la entrada. Un kit de jugadores vacío se informa como opcional.

Si un equipo activo tiene configuración de inicio incompleta, se indican los fallos antes de mover jugadores. Para iniciar solo un equipo concreto, utiliza inicio comenzar <equipo>.

## Máquinas

Se retiró todo el grupo /nightminigames maquina. La máquina sigue disponible dentro de /nightminigames kit configuracion <equipo> [jugador], ya vinculada al equipo; no se elimina el bloque ni su mecánica.

## Puntos

/nightminigames puntos equipo <equipo> <cantidad>

Entrega directamente la cantidad POR INTEGRANTE del equipo actual, sin vista previa ni confirmar. La cantidad admitida es de 1 a 10000. No requiere clasificación ni utiliza su snapshot. Incluye miembros desconectados si se puede resolver su UUID en la caché de perfiles. Sigue requiriendo MystrixPoints y conserva el control de pago único por equipo/ronda para evitar duplicados. puntos estado, lista y diagnostico permanecen disponibles.

Ejemplo: /nightminigames puntos equipo rojo 100

## HUD

Solo quedan estas funciones del grupo HUD:

- /nightminigames hud limpiar: limpia los historiales de notificaciones generales y de clasificación para todos, sin borrar la clasificación real de la ronda.
- /nightminigames hud estado: informa de ambos historiales.
- /nightminigames hud clasificacion limite <lineas>: mantiene el límite de 1 a 8 líneas.

Se retiraron hud clasificacion limpiar y hud clasificacion estado.

## Otros comandos retirados

- /nightminigames estado
- /nightminigames persistencia estado
- /nightminigames persistencia dimensiones
- /nightminigames persistencia linterna recargar
- /nightminigames op informe
- /nightminigames op estado

La persistencia automática interna sigue funcionando. La comprobación de configuración se concentra en inicio comprobar.

## Instalación

Usar el JAR server en el servidor y el JAR client en los clientes; reemplazar la versión anterior del mismo mod. Las dependencias del proyecto se mantienen: Fabric 1.21.1, Java 21, Fabric Loader >=0.19.5, Fabric API y GeckoLib >=4.9.3; el cliente también necesita Veil Mystrix >=4.5.0. MystrixPoints y MapaMystrix siguen siendo integraciones opcionales.

Esta nota describe la versión actual. Las notas anteriores del proyecto conservan información histórica y pueden contener comandos retirados.
