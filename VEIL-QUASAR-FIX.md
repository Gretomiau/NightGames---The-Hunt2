# Quasar: corrección del paquete de pruebas

El JAR Veil Mystrix incluido en este proyecto registra tres paquetes de recursos opcionales: `test_shaders`, `fog` y `black_hole`.

Se ha desactivado únicamente el registro automático de `test_particles`, que contenía un recurso experimental `veil:rocket` cuya carga producía errores en el registro de partículas Quasar. Sus archivos JSON permanecen dentro del JAR y no se han modificado los shaders, los renderizadores de luces ni el resto de las clases de Veil.

El cambio se aplica en `foundry.veil.impl.VeilBuiltinPacks` y su código fuente está en `patches/veil/foundry/veil/impl/VeilBuiltinPacks.java`.

La copia modificada del JAR se guarda en `libs/` y en `run/mods/`. La tarea `prepareRunMods` utilizará la copia modificada de `libs/` al iniciar el cliente.

Si en una instalación existente se habían seleccionado manualmente las partículas de ejemplo, hay que desactivarlas desde Opciones > Paquetes de recursos. Los ejemplos ya no se registran automáticamente desde Veil.

La linterna de Nightminigames sigue usando las luces de Veil y no depende del paquete `test_particles`.

La integridad del JAR y del ZIP ha sido revisada. No se ha realizado una prueba de ejecución de Minecraft en este entorno.
