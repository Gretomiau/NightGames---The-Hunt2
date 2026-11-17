# Nightminigames — HUD de clasificación separado

## Distribución

El historial superior izquierdo mantiene exclusivamente recogidas de llaves y cambios de progreso de las máquinas. No utiliza paneles ni fondos. Muestra como máximo las 7 entradas más recientes.

Debajo aparece otro historial únicamente para clasificación y cupos. Tiene texto sin fondo, conserva los colores de cada equipo y muestra las 5 entradas más recientes de forma predeterminada. Las líneas anteriores salen de pantalla conforme entran otras nuevas. El límite visible se puede cambiar entre 1 y 8.

## Comandos OP

```mcfunction
/nightminigames hud clasificacion limpiar
/nightminigames hud clasificacion estado
/nightminigames hud clasificacion limite 5
/nightminigames hud limpiar
```

El último comando borra el historial superior de llaves y máquina; no borra el de clasificación.

## Almacenamiento

- `world/data/nightminigames_hud_history.json`: historial superior original.
- `world/data/nightminigames_classification_hud.json`: historial separado, máximo 20 eventos guardados, límite visible configurable.

La primera apertura de un mundo existente intenta migrar eventos antiguos de clasificación que se encontraban mezclados en el historial superior, sin duplicarlos en la interfaz.

No cambia los cupos de clasificación, la selección de equipos, las puertas, los spawners ni las demás reglas del minijuego.
