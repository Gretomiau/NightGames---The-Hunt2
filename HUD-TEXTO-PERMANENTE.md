# Historial HUD en texto permanente

- Las recogidas de llaves y las inserciones en la máquina aparecen como texto en la esquina superior izquierda, sin panel, marco ni fondo.
- Los colores de cada llave y los nombres de jugadores se conservan.
- Las entradas no tienen temporizador ni desaparición automática.
- El historial se sincroniza con todos los jugadores y se recupera al volver a conectarse.
- El archivo de historial se guarda en `data/nightminigames_hud_history.json` dentro de la carpeta del mundo del servidor.
- Se conservan hasta las últimas 120 entradas para limitar memoria y tráfico de red. Las anteriores se reemplazan por las nuevas.
- Si no caben todas en la pantalla, se muestran las más recientes sin borrar el historial.

## Comandos OP

- `/nightminigames hud limpiar`: borra el historial para todos los jugadores y también su archivo persistente.
- `/nightminigames hud estado`: consulta cuántas entradas están almacenadas.

## Linterna

Se retiró el texto `SISTEMA DE ENERGIA // VEIL INTERFACE` del widget de carga, sin tocar su función de 5 segundos, diseño ni posición.
