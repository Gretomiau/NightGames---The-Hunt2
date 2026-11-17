# Correcciones de linterna

- Modelo de primera persona ajustado para evitar que el cabezal ocupe el centro de la pantalla.
- La luz de Veil se desplaza fuera del ojo/cámara para reducir la sobreiluminación de la propia linterna.
- Se añadió la animación vanilla `BRUSH` de 12 ticks al usar el ítem. Se activa en cada interacción permitida, tanto al encender como al apagar.
- Intervalo entre activaciones: 12 ticks (0,6 s a 20 TPS), validado por el servidor.
- El modelo y la textura provienen del ZIP original de Flashgame.
- Los avisos `lightsystem:veil_light` corresponden a un mundo guardado con el mod anterior: haz copia de seguridad antes de abrirlo sin ese mod. Para pruebas usa un mundo nuevo.
- El log masivo de `Finished uploading vanilla shaders` procede del Veil Mystrix y no se corrige aquí; se debe diagnosticar con sus fuentes para evitar alterar su lógica de renderizado.

Prueba primero con un mundo nuevo y `./gradlew runClient`. Da la linterna con `/give @s nightminigames:flashlight`.

## Ajuste de tamaño en primera persona

Se restauró el tamaño de la linterna en primera persona a 0.57. Se elevó desde Y=-4.4 hasta Y=0.6 y se acercó desde Z=-5.8 hasta Z=-1.8 para que el modelo vuelva a ser visible sin ocupar toda la pantalla. Ambos brazos utilizan transformaciones simétricas.
