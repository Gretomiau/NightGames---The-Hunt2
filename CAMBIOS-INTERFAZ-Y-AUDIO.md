# Nightminigames — Corrección de GUI y audio

Se conserva el píxel `FULL_BRIGHT` del lightmap para que la oscuridad del mundo no afecte a textos, menús, inventario ni hotbar.

La pista ambiental original `ambient/loop.ogg` permanece intacta. El reproductor utiliza `Master` y una sola instancia en bucle; deja de reiniciarse mientras el sonido se carga.

Los archivos originales `flashlight/toggle.ogg` y `ambient/power_on.ogg` estaban corruptos. Se sustituyeron por un nuevo clic sintetizado y por una versión invertida y remasterizada del efecto válido `power_off.ogg`, respectivamente.

La linterna, la música y el efecto de apagado usan `Master`; el control global de volumen de Minecraft sigue aplicándose.

Comandos OP para probar el sistema:

```mcfunction
/nightminigames sonido probar vanilla
/nightminigames sonido probar linterna
/nightminigames sonido probar encender
/nightminigames sonido probar apagar
/nightminigames musica activar
/nightminigames musica volumen 80
/nightminigames oscuridad activar
```

Si no suena la prueba vanilla, revisar la salida de audio del sistema operativo y el control Master de Minecraft.
