# Nightminigames — HUD Veil GLSL

Los cuatro HUD utilizan el programa dinámico `nightminigames:hud/liquid_ui` compilado mediante el `ShaderManager` de Veil Mystrix.

- `FlashlightChargeHud`: energía, reflejos y progreso de activación de 5 segundos.
- `KeyHoldClient`: extracción de llave, color correspondiente y progreso de 6 segundos.
- `MachineProgressHud`: notificaciones de llaves insertadas y estado de máquina.
- `KeyAnnouncementHud`: formación local de líquido, explosión y revelación de la cabeza y nombre del jugador.

Los efectos se calculan en GLSL 330 a resolución física del framebuffer y se adaptan al tamaño de GUI. Cada widget solicita el shader únicamente mientras está visible. Si el programa no está disponible o falla su compilación, se dibuja un fondo alternativo con `GuiGraphics` sin interrumpir el juego.

Recursos:

- `src/main/resources/assets/nightminigames/shaders/hud/liquid_ui.vsh`
- `src/main/resources/assets/nightminigames/shaders/hud/liquid_ui.fsh`

Entrada de render:

- `src/main/java/dev/greto/nightminigames/client/VeilHudShader.java`

El procesamiento es exclusivamente gráfico en el cliente. Las validaciones de recogida, uso, batería, comandos y sincronización permanecen en el servidor.

Para compilar con JDK 21 y las dependencias descargadas:

```powershell
.\gradlew.bat --no-daemon clean build
.\gradlew.bat --no-daemon runClient
```

Esta versión se ha validado estructuralmente. El shader GLSL y la compilación Java requieren pruebas dentro del cliente Minecraft porque la distribución de Gradle no se encuentra disponible en este entorno.
