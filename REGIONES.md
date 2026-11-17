# Regiones Entrada y Salida

Las dos regiones se seleccionan mediante varitas y únicamente los operadores pueden configurarlas.

- `/nightminigames regiones wand entrada`
- `/nightminigames regiones wand salida`
- `/nightminigames regiones estado`
- `/nightminigames regiones limpiar entrada`
- `/nightminigames regiones limpiar salida`

Con la varita seleccionada, haz clic izquierdo sobre un bloque para marcar Pos1 y clic derecho sobre otro bloque para marcar Pos2. El contorno se representa en el cliente mientras sostienes cualquiera de las dos varitas: Entrada en cian y Salida en naranja. Cada región utiliza un cuboide tridimensional, incluyendo las coordenadas Y de ambos puntos, y está asociada a la dimensión en que se configuró.

Cuando un jugador cruza del exterior al interior de una región aparece un mensaje en el chat de todos los jugadores. Permanecer dentro no repite el aviso. Los avisos no se activan retroactivamente al configurar una región ni por cargar una partida con el jugador dentro.

La configuración persiste en `world/data/nightminigames_regions.json`. Este módulo no teletransporta jugadores, no inicia partidas y no modifica las mecánicas de las llaves o de la máquina.
