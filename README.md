# NightGames---The-Hunt2

Proyecto Nightminigames para Minecraft Fabric 1.21.1. Versión actual: **1.0.8**.

Incluye código fuente, recursos, Gradle Wrapper y dependencias locales necesarias para compilar. Consulta [terror 3D, flotantes y ticker](TERROR-3D-FLOTANTE-Y-TICKER-1.0.8.md) para los cambios más recientes.

## Compilación actual

Gradle/Loom requieren **JDK 25** para ejecutar la compilación; las clases del mod y Minecraft usan **Java 21**. Configura JAVA_HOME con un JDK 25 y conserva un JDK 21 instalado para el toolchain.

```bat
gradlew.bat build
```

Los JAR separados de cliente y servidor se generan en `libs/`. Los mundos, cachés, registros y JAR generados no se versionan. El cliente necesita Fabric API, GeckoLib y el Veil personalizado incluido en `libs/`; el JAR de servidor se empaqueta sin las clases de renderizado ni dependencia de Veil.

## Documentación previa

Los apartados siguientes conservan notas históricas; algunos describen versiones anteriores. La versión de referencia actual está en `gradle.properties`.

> Versión actual 1.0.5: [waypoints de máquinas habilitados por OP](WAYPOINTS-MAQUINAS-1.0.5.md).

> Versión actual 1.0.4: [vista personal de evaluación de llaves](EVALUACION-LLAVES-1.0.4.md).

> Versión actual 1.0.3: [waypoints de llaves con luces y clasificación](WAYPOINTS-LLAVES-1.0.3.md).

> Versión actual 1.0.2: [devolución de llaves al morir](DEVOLUCION-LLAVES-1.0.2.md).

> Versión actual 1.0.1: consulta [COMANDOS-E-INICIO-1.0.1.md](COMANDOS-E-INICIO-1.0.1.md) para los cambios de inicio, teletransporte, puntos y comandos retirados.

# Nightminigames — Fabric 1.21.1

## Requisitos

- JDK 21 para compilar y ejecutar Minecraft 1.21.1
- Gradle Wrapper 9.7.1
- Internet para resolver Fabric API y GeckoLib por primera vez

## Dependencias

- Fabric Loader 0.19.5
- Fabric API 0.116.8+1.21.1
- GeckoLib 4.9.3
- Veil 4.5.0+mystrix.1 (JAR personalizado incluido)

El `lightsystem-1.0.0.jar` original está en `references/`, no en `run/mods`. Ese JAR usa llamadas que no existen en la versión personalizada de Veil y provoca un `NoSuchMethodError`. El sistema de linterna de Nightminigames usa las luces `AreaLightData` de Veil directamente.

## IntelliJ IDEA

Importa la carpeta como proyecto Gradle. Sincroniza Gradle y ejecuta `Nightminigames Client (Fabric 1.21.1)` o:

```powershell
.\gradlew.bat --no-daemon runClient
```

`prepareRunMods` instala los JAR de desarrollo y elimina copias incompatibles de LightSystem dentro de `run/mods` y `run-server/mods`.

## Cuatro llaves originales de Flashgame

Se registraron cuatro items de GeckoLib en la pestaña creativa **Nightminigames**, con la geometria, texturas y transformaciones de primera/tercera persona originales. Todavia no se han integrado spawners, cerraduras ni el sistema de puzzle.

```mcfunction
/give @s nightminigames:key_blue
/give @s nightminigames:key_yellow
/give @s nightminigames:key_red
/give @s nightminigames:key_green
```

## Linterna

La linterna es `nightminigames:flashlight` y figura tanto en la pestaña creativa **Nightminigames** como en **Herramientas y utilidades**.

```mcfunction
/give @s nightminigames:flashlight
/nightminigames linterna dar
/nightminigames linterna dar <jugador>
/nightminigames estado
```

Usa clic derecho para encender o apagar la linterna. La luz es cliente/GPU y el estado y consumo se sincronizan mediante el servidor. Su autonomía es de aproximadamente **480 segundos (8 minutos)** de encendido continuo a 20 TPS, cuatro veces la duración anterior.

Recarga de linternas (solo OP):

```mcfunction
/nightminigames linterna bateria recargar
/nightminigames linterna bateria recargar <jugador>
/nightminigames linterna bateria recargar todos
```

También se acepta `resetear` como sinónimo de `recargar`. Cada comando recarga todas las linternas del inventario indicado, incluida la segunda mano. La recarga no enciende automáticamente linternas apagadas. Para las linternas existentes, el nuevo máximo de durabilidad también es 480; para llenarlas al 100 %, usa el comando.

## Diagnóstico del registro anterior

- `java.net.SocketTimeoutException` aparece durante la consulta del perfil Minecraft en `sessionserver.mojang.com`; no implica que Gradle esté fallando al descargar dependencias.
- `NoSuchMethodError` aparece cuando LightSystem intenta ejecutar `AreaLightData.setPosition(double,double,double)`, ausente en Veil Mystrix.
- El ítem estaba registrado pero no se añadía a ningún grupo creativo.

La compilación y el inicio con todas las dependencias externas deben verificarse localmente.


## Corrección de Quasar

El paquete opcional `veil:test_particles` de Veil Mystrix no se registra automáticamente. Se conservan los otros paquetes de Veil. Ver `VEIL-QUASAR-FIX.md`.


## Oscuridad total y música ambiental

Al entrar a un mundo, **oscuridad y música están activadas por defecto**. El servidor sincroniza ambos estados a todos los jugadores que tengan el mod cliente.

La oscuridad actúa sobre la lightmap del cliente (bloques, entidades y equipamiento que usan esa iluminación) y oscurece el cielo y las nubes. El haz de la linterna mantiene su iluminación dinámica con Veil. No es un shader de pantalla completa: se evita oscurecer el propio haz de Veil. Los materiales emisivos, elementos de interfaz y algunos efectos de otros shaders pueden permanecer visibles.

La pista original de Flashgame está en `assets/nightminigames/sounds/ambient/loop.ogg`, dura 48.1 segundos y se repite con audio en streaming. Se reproduce en la categoría **Música**; si no se escucha, verifica Opciones → Música y sonidos → Música, además del volumen general. No genera entidades de audio en el servidor.

Comandos disponibles (solo operadores):

```mcfunction
/nightminigames oscuridad activar
/nightminigames oscuridad desactivar
/nightminigames oscuridad estado
/nightminigames musica activar
/nightminigames musica desactivar
/nightminigames musica volumen 45
/nightminigames musica estado
/nightminigames linterna dar
/nightminigames linterna bateria recargar
```

Configuración persistente en `config/nightminigames/environment.json`, con campos `darkness`, `music` y `volume` (de 0.0 a 1.0). La configuración se aplica tras reiniciar. Los comandos se sincronizan inmediatamente.

Esta oscuridad es **visual**: no cambia la hora del servidor, las reglas de aparición de mobs ni la iluminación lógica para cultivos o redstone. La linterna conserva la batería x4, su modelo y el seguimiento de mira previos. Las luces Veil se renderizan en el cliente; el servidor solo guarda y sincroniza estados.

Para iniciar en IntelliJ:

```powershell
.\gradlew.bat --no-daemon clean runClient
```

No reinstalar `lightsystem-1.0.0.jar` en `run/mods`: sigue siendo incompatible con la API de Veil Mystrix.


## Linterna: carga de activación sci-fi

Mantén clic derecho durante 5 segundos con la linterna apagada y con batería. Un HUD rectangular futurista, ubicado por encima de la action bar, muestra la barra segmentada, un punto luminoso y el porcentaje de activación. Si sueltas el botón, cambias de objeto, la carga se interrumpe. Al alcanzar los 100 ticks de uso, el servidor enciende la linterna exactamente una vez; el jugador puede seguir manteniendo el botón sin alternarla repetidamente. Para apagarla, basta pulsar clic derecho de nuevo. La animación del ítem durante el uso es BRUSH; la luz y los sonidos existentes permanecen.

El HUD se dibuja únicamente en el cliente con la tubería de renderizado GUI de Minecraft (gradientes, brillo pulsante, segmentos y marcador), sin superponer un shader de pantalla completa que oscurezca los textos. El renderizado del mundo sigue utilizando Veil Mystrix.
