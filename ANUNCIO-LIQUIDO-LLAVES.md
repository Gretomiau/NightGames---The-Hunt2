# Anuncio global de llaves — Nightminigames

La recogida de una llave después de mantener clic derecho durante 6 segundos envía un anuncio visual a todos los clientes conectados.

## Animación

1. Una ola líquida del color de la llave atraviesa la pantalla de izquierda a derecha.
2. La ola estalla en un anillo expansivo y gotas coloreadas.
3. Aparece en el centro una tarjeta sci-fi con la cabeza de la skin del jugador, su nombre y el color de la llave.
4. El anuncio desaparece suavemente.

Duración aproximada: 5,1 segundos. Si se recogen varias llaves al mismo tiempo, se muestran sucesivamente. El efecto utiliza renderizado de GUI y no modifica la cámara, los shaders de Veil, la iluminación del mundo ni las posiciones de los objetos.

La textura de la cabeza se obtiene de la información de skins del cliente. Si no está disponible, se utiliza una skin predeterminada correspondiente al UUID.

El anuncio original del chat global y los tres efectos de sonido de las llaves se conservan. Los comandos que entregan llaves directamente no activan el HUD.

Los colores, dimensiones y posiciones se calculan en las coordenadas escaladas de la GUI, por lo que el efecto se adapta a distintas resoluciones y escalas de interfaz.
