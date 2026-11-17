# Nightminigames — cierre de partida, puntos y distribución por lados

## Generar dos JAR

En IntelliJ IDEA con JDK 21 como toolchain de Minecraft, usa Gradle:

```powershell
.\gradlew.bat --no-daemon clean build
```

Al terminar correctamente, encontrarás:

```text
libs/nightminigames-client-1.0.0+1.21.1.jar
libs/nightminigames-server-1.0.0+1.21.1.jar
```

El JAR de cliente contiene recursos, HUD, efectos y renderizadores, además de la lógica compartida necesaria. Se usa exclusivamente en el cliente y requiere Veil Mystrix, GeckoLib y Fabric API.

El JAR de servidor contiene lógica común, equipos, puertas, clasificación, persistencia, puntos y control de spawners. Excluye clases cliente y mixins cliente; requiere Fabric API y GeckoLib, pero no Veil.

No pongas ambos JAR de Nightminigames en la misma carpeta `mods`: el cliente usa el de cliente y el servidor dedicado usa el de servidor. Conserva las dependencias de cada lado. MystrixPoints es opcional para iniciar, pero obligatorio en el servidor para otorgar puntos.

## Clasificados y fin de partida

```mcfunction
/nightminigames clasificacion cupos 4
/nightminigames clasificacion estado
/nightminigames clasificacion lista
```

Al llenarse los cupos, la ronda pasa a FINALIZADA y se conserva así al reiniciar el servidor. Los OP reciben un resumen y la lista numerada de los equipos que clasificaron. El sistema bloquea nuevos comienzos, teletransportes de módulos y acciones de recogida/inserción de la partida. El servidor Minecraft continúa funcionando normalmente.

## Puntos manuales

```mcfunction
/nightminigames puntos equipo amarillo
/nightminigames puntos equipo amarillo 10
/nightminigames puntos equipo amarillo 10 confirmar
```

La entrega de puntos no ocurre automáticamente y continúa disponible cuando la ronda terminó. Cuando hay snapshot histórico, se utiliza ese conjunto inmutable de UUID. Si una clasificación antigua no tiene snapshot, se propone la plantilla actual identificable del equipo. **El OP debe revisar cuidadosamente la vista previa**, porque la plantilla puede haber cambiado desde la clasificación e incluir jugadores que antes estaban desconectados o en otro modo. Los nombres sin UUID resoluble no se incluyen. La entrega tiene identificador único por ronda/equipo y no se repite.

## Reinicio de spawners

```mcfunction
/nightminigames spawners reiniciar
```

Reinicia solamente los spawners de llaves y linternas. Los chunks descargados no se cargan; se aplicará el reinicio cuando vuelvan a estar cargados. Se procesan hasta ocho estructuras por tick. Las máquinas y las clasificaciones permanecen intactas.

## Reinicio de la partida

```mcfunction
/nightminigames inicio reiniciar
```

Cancela la ronda terminada y restablece puertas, máquinas, clasificación, contadores y entregas de kit. También reinicia los spawners existentes, incluidos los descargados cuando vuelvan a cargarse. Conserva regiones, destinos y configuraciones. No retira automáticamente los ítems de los inventarios y no borra puntos ya acreditados por MystrixPoints.

## Limitaciones de la verificación

Se revisó sintaxis Java y validez de JSON. No se pudo ejecutar Gradle ni comprobar ambos JAR en juego porque el entorno no pudo resolver el servidor de distribución de Gradle.
