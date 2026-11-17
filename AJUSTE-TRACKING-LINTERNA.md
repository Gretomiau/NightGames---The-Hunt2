# Nightminigames — Seguimiento de linterna

La linterna conserva el modelo y las transformaciones originales de Flashgame, sin ajustes de escala.

El foco ahora parte de una posición cercana a la mano seleccionada, alineada con los vectores de la cámara renderizada. El cliente hace una traza de bloques desde la mira, calcula un punto objetivo y orienta ambos emisores Veil hacia él. Esto corrige la separación entre la luz y el punto al que apunta el jugador, especialmente a corta distancia.

La luz recupera presencia respecto de la revisión anterior: alcance 48 bloques, núcleo 5.0 y dispersión 1.65, con conos de 28° y 65°. Cuando una superficie está muy cerca, la intensidad se atenúa progresivamente para no quemar paredes; no se modifica la iluminación global de Minecraft ni los bloques.

El parche de Veil Mystrix contra el spam de `Finished uploading vanilla shaders`, el registro del ítem, la animación de uso y el control de batería permanecen integrados.

Ejecutar con Java 21 y `gradlew.bat --no-daemon clean runClient` desde IntelliJ IDEA. Revisar visualmente el comportamiento a distancias cortas, medias y largas; no se ha podido ejecutar una prueba gráfica dentro de este entorno.
