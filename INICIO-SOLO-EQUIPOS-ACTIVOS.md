# Inicio global con equipos parcialmente ocupados

Nightminigames no exige que los siete equipos estén llenos ni que los siete tengan participantes conectados.

## Flujo del OP

```mcfunction
/nightminigames equipos autoasignar
/nightminigames modulos teleportar todos
/nightminigames inicio comenzar
```

El inicio global abre simultáneamente las puertas Entrada de los equipos que tengan al menos un participante conectado en Survival o Adventure que haya llegado a su módulo.

Los equipos sin participantes activos quedan fuera del inicio y sus puertas permanecen cerradas. No necesitan un kit, destino, región ni puerta configurados para que comiencen los demás. Los kits de jugadores vacíos son válidos.

Los participantes conectados sin equipo sí deben asignarse antes de la distribución. Los jugadores de equipos participantes necesitan su destino cargado y su puerta y región Entrada configuradas. El inicio no avanza si un participante válido sigue sin llegar a su módulo.

Si un jugador que estaba en el lote se desconecta o pasa a Creative/Spectator antes del inicio, deja de contar para desbloquear la apertura. Con cero participantes válidos conectados, el comando rechaza comenzar, en lugar de iniciar una ronda vacía accidentalmente.

## Ejemplos

- Solo Azul, un jugador conectado: comienza Azul.
- Rojo y Amarillo con jugadores; los otros cinco vacíos: comienzan Rojo y Amarillo.
- Seis jugadores repartidos en tres equipos, dos se desconectan y queda al menos un participante: los conectados que hayan llegado pueden comenzar.
- Todos los equipos sin participantes: el inicio se rechaza y se explica el motivo.

## Diagnóstico

```mcfunction
/nightminigames inicio estado
/nightminigames inicio diagnostico
```

Los mensajes de inicio se dirigen a los OP. No se cambian los sistemas de clasificación, puntos, regiones, spawners ni el empaquetado cliente/servidor.
