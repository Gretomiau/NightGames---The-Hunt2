# Nightminigames — Luz de linterna y Veil

## Seguimiento del haz

En primera persona, la posición del emisor utiliza el origen de la cámara renderizada, con un pequeño desplazamiento hacia el lado de la mano que sostiene la linterna. La orientación apunta hacia la intersección de la mira con los bloques del mundo; si no hay impacto, sigue la dirección de la cámara hasta el alcance configurado.

Esto alinea la iluminación con la mirada, mantiene el haz cerca del jugador y reduce el desplazamiento visual a distancias cortas. En tercera persona se mantiene el comportamiento asociado al jugador, sin emitir una luz desde la cámara externa.

## Intensidad y cobertura

Dos emisores de área Veil: 48 bloques de alcance, intensidades 5.0 y 1.65, ángulos 28° y 65°. Se conserva una reducción de potencia a distancias muy cortas contra obstáculos para evitar zonas completamente blancas. El modelo original de Flashgame conserva sus transformaciones y escala de primera persona.

## Parche de Veil

La librería Veil Mystrix incluida mantiene la corrección aplicada previamente a `DynamicBufferManager.endFrame()`, para no informar de cargas de shaders finalizadas cuando la cola aún tiene shaders. El código del parche está en `patches/veil/PatchDynamicBufferManager.java`.

## Ejecución

Desde IntelliJ IDEA, ejecutar `runClient`, o ejecutar `gradlew.bat --no-daemon clean runClient` con Java 21. Verificar la iluminación en una pared a menos de 1 bloque, a 3 bloques y a 10 o más bloques. El compilado y la verificación gráfica de esta revisión están pendientes porque la descarga de Gradle no estaba disponible en el entorno de preparación.
