# Nightminigames — Holograma, anuncio 4/4 y equipos

## Holograma de linterna

El holograma del spawner conserva la escala 2.60 y ahora está a Y + 0.72 sobre su bloque (antes Y + 1.52). No se cambiaron la textura, geometría ni el cubo de selección invisible.

## Título personalizado

Cuando un jugador inserta la cuarta llave de una máquina, todos los clientes reciben una única notificación y ven durante 6.8 segundos:

**¡MÁQUINA COMPLETADA!**

¡Completaste! Ahora todos los jugadores diríjanse a la puerta para completar el minijuego.

Usa el shader de Veil existente para el fondo animado cuando está disponible y una alternativa con GuiGraphics si falla. El HUD se ajusta a la resolución y GUI Scale. La viñeta verde y los avisos anteriores no se eliminaron.

## Equipos de Minecraft

Se utilizan los equipos nativos del scoreboard del servidor. Sus miembros, colores y reglas se guardan automáticamente en el mundo.

```mcfunction
/nightminigames equipos crear rojo
/nightminigames equipos crear azul azul
/nightminigames equipos crear verde verde
/nightminigames equipos asignar <jugador> rojo
/nightminigames equipos quitar <jugador>
/nightminigames equipos autoasignar
/nightminigames equipos lista
/nightminigames equipos estado
/nightminigames equipos estado <jugador>
/nightminigames equipos eliminar rojo
```

La autoasignación reparte únicamente a jugadores conectados en Supervivencia/Aventura que todavía no tengan equipo. No incluye Creativo ni Espectador. Al crear un equipo se desactiva el daño entre compañeros.

## Máquinas por equipo

Un OP puede apuntar a una máquina y utilizar:

```mcfunction
/nightminigames maquina vincular rojo
/nightminigames maquina desvincular
```

Si la máquina tiene un equipo vinculado, solo los integrantes pueden insertar sus llaves. Un OP puede hacerlo para pruebas. La máquina guarda el ID de equipo en su BlockEntity. Las máquinas sin vinculación siguen siendo compartidas. Cuando se completan, el anuncio global incorpora el equipo y su color.

Las puertas y las dos regiones continúan compartidas, exactamente como en la versión anterior. Esta entrega no configura automáticamente un recorrido o una puerta diferente para cada equipo.

## Comprobación

```powershell
.\gradlew.bat --no-daemon clean build
.\gradlew.bat --no-daemon runClient
```

La compilación completa y la validación visual dentro de Minecraft todavía deben comprobarse en el equipo del usuario.
