# Reparación del error de Gradle / Loom en Nightminigames

Se corrigió `gradle/wrapper/gradle-wrapper.properties`: ahora usa Gradle **9.7.1** en lugar de 9.5.1.

**Motivo:** Loom 1.18.4 declara `org.gradle.plugin.api-version=9.7.0` y el Gradle 9.5.1 original no puede resolverlo.

## IntelliJ IDEA (Windows)

1. Instala JDK 25 (para ejecutar Gradle/Loom) y JDK 21 (para compilar Minecraft 1.21.1).
2. Ve a **Settings > Build, Execution, Deployment > Build Tools > Gradle** y selecciona **Gradle JVM: JDK 25**.
3. Haz clic en **Reload All Gradle Projects** (icono de actualización en la ventana Gradle).
4. En la raíz de proyecto, en una terminal configurada con JDK 25:

   ```powershell
   .\gradlew.bat --version
   .\gradlew.bat --no-daemon clean build
   .\gradlew.bat --no-daemon runClient
   ```

El resultado esperado para la primera línea es **Gradle 9.7.1 / JVM 25**.

Si IntelliJ indica que no hay un compilador Java 21, instálalo y regístralo en **Project Structure > SDKs**. No cambies `java.toolchain` a 25: el mod debe compilar para Java 21.

La primera sincronización descarga Gradle, Loom, Minecraft y las dependencias externas.
