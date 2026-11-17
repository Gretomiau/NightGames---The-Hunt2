# Nightminigames — Siete equipos y anuncio de salida

## Equipos

Los siete equipos predefinidos son `rojo`, `azul`, `verde`, `amarillo`, `naranja`, `morado` y `rosa`.

```mcfunction
/nightminigames equipos preparar
/nightminigames equipos autoasignar
/nightminigames equipos lista
/nightminigames equipos asignar <jugador> <equipo>
/nightminigames maquina vincular <equipo>
```

`preparar` conserva los equipos existentes. `autoasignar` solo distribuye jugadores conectados en Supervivencia/Aventura que todavía no tienen equipo. Ambos comandos son exclusivos de OP.

## Máquina y título

Cuando una máquina alcanza 4/4, el aviso **«¡MÁQUINA COMPLETADA!»** permanece en la pantalla de los integrantes del equipo al que pertenece esa máquina. Si no está vinculada, se usa el equipo del jugador que insertó la última llave.

La notificación desaparece cuando **todos los integrantes registrados en ese equipo** están conectados, vivos, no están en Espectador y se encuentran **simultáneamente dentro de la región Salida en la dimensión de la máquina**. Los desconectados no cuentan como presentes; la llegada de un solo jugador no finaliza el aviso.

La puerta de Salida se cierra después de cumplirse esa condición, no cuando entra el primer integrante. La protección contra colisiones con jugadores se conserva.

Si una máquina no está asociada a ningún equipo, el anuncio sigue siendo temporal para quien coloca la cuarta llave, conservando la compatibilidad anterior.

El seguimiento del equipo se guarda en `world/data/nightminigames_team_exit.json` y se recupera al reconectar. Reiniciar o eliminar la máquina cancela el anuncio que le corresponda.
