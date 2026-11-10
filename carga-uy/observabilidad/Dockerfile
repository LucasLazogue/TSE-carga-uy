# Plataforma de Observabilidad: colector de OpenTelemetry, Prometheus (metricas), Tempo (trazas), Loki (logs) y
# Grafana en una sola imagen, mas la configuracion de carga.uy
FROM grafana/otel-lgtm:0.34.0

COPY otelcol-carga-uy.yaml /otel-lgtm/otelcol-carga-uy.yaml
COPY dashboards.yaml /otel-lgtm/grafana/conf/provisioning/dashboards/carga-uy.yaml
COPY dashboards/ /otel-lgtm/carga-uy-dashboards/

# OpenShift corre el contenedor con un usuario arbitrario del grupo 0: los componentes escriben en /data (Grafana,
# Prometheus, Loki, Tempo), en /var/tempo y en /etc/lgtm, asi que el grupo 0 tiene que poder escribir ahi
RUN mkdir -p /data /var/tempo /etc/lgtm \
    && chgrp -R 0 /data /var/tempo /etc/lgtm \
    && chmod -R g=u /data /var/tempo /etc/lgtm

# la imagen arranca el colector con su configuracion y le suma esta
ENV OTELCOL_EXTRA_ARGS="--config=file:/otel-lgtm/otelcol-carga-uy.yaml"

# 3000: Grafana; 4317 y 4318: OTLP (gRPC y HTTP); 4319: OTLP HTTP con token, para los nodos de otros proyectos
EXPOSE 3000 4317 4318 4319
