# Correcciones del artículo

Este archivo reúne lo que falta corregir en el artículo técnico (`docs/articulo`) antes de la entrega
final (16/11/2026). Así el LaTeX queda limpio, sin marcas en rojo, y se puede entregar en cualquier
momento.

**Cómo usarlo:** cuando algo se resuelve en el artículo, se borra de acá en la misma PR.

**Estado:** versión preliminar del 2/11/2026.

## Para la entrega final

1. **Largo.** La letra pide entre 8 y 12 páginas para Ingeniería (Entrega Final, punto 4), y el
   artículo tiene 17. Hay que recortar unas 2.000 palabras. Consultar en el monitoreo si la
   bibliografía (casi 2 páginas) entra en el límite. Dónde hay más para recortar:
   - la tabla de productos de Detalles de Implementación (13 filas, casi una página y media);
   - Solución Propuesta (~1.800 palabras), que repite parte del documento de arquitectura;
   - la Introducción (~730 palabras).
2. **Prueba de performance.** Reemplazar la Tabla "Prueba preliminar de la ingesta" y su párrafo
   por los resultados de la prueba en la nube: volumen objetivo y pico, frontoffice bajo carga
   (AC09, AC10), punto de quiebre y cuellos de botella (AC11) y escalado con varias réplicas
   (opcional 4). Ver `docs/performance.md`.
3. **Despliegue.** Actualizar "Despliegue sin Elastic Cloud" (Problemas Encontrados) cuando se
   valide la conexión de los nodos de tracking al broker con TLS y SNI, o si el central pasa a
   Elastic Cloud.
4. **Cantidad de PRs.** Desarrollo del Proyecto dice que se integraron 81 entre el 6/10 y el 2/11.
   Actualizar el número y la fecha al cierre.
5. **Puntos débiles y trabajo a futuro.** Revisar la lista de Evaluación de la Solución y de
   Conclusiones contra lo que se implemente antes del 16/11 (desvío de ruta, PDI, correo, filtro
   por ubicación, viajes solapados).

## Decisiones tomadas

- No se incluye el porcentaje de tiempo por tarea ni la comparación con el cronograma que sugiere la
  plantilla en Desarrollo del Proyecto (2/11/2026). La letra no lo pide.
