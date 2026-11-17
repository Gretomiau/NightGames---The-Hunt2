# Bloque KILL y protección de objetos

Bloque invisible `nightminigames:kill`, sin colisión ni luz. Al atravesarlo, mata a jugadores Survival/Adventure y a criaturas vivas. Creativo y Espectador quedan excluidos; no destruye objetos tirados. El marcador rojo aparece solo en Creativo. Al activar `show`, muestra también su nombre.

```mcfunction
/give @s nightminigames:kill
/setblock ~ ~ ~ nightminigames:kill
```

WorldEdit:

```mcfunction
//set nightminigames:kill
//replace minecraft:air nightminigames:kill
```

Coloca el bloque con `//set` en una selección. El icono es exclusivamente visual y no se conserva como entidad; solo aparece para jugadores en Creativo a una distancia limitada. Evita colocar grandes cantidades innecesarias: el bloque KILL no crea BlockEntities ni necesita ticks de bloque, y el cliente limita la cantidad de marcadores que dibuja.

Las cuatro llaves y la linterna no pueden tirarse manualmente con Q/Ctrl+Q ni desde ranuras o desde el cursor del inventario. Las operaciones administrativas y las mecánicas de muerte permanecen sin cambios.
