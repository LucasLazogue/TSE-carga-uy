# Plataforma de Observabilidad: colector de OpenTelemetry, Prometheus (metricas), Tempo (trazas), Loki (logs) y
# Grafana en una sola imagen, mas la configuracion de carga.uy
FROM grafana/otel-lgtm:0.34.0

COPY otelcol-carga-uy.yaml /otel-lgtm/otelcol-carga-uy.yaml
COPY dashboards.yaml /otel-lgtm/grafana/conf/provisioning/dashboards/carga-uy.yaml
COPY dashboards/ /otel-lgtm/carga-uy-dashboards/

# la imagen arranca el colector con su configuracion y le suma esta
ENV OTELCOL_EXTRA_ARGS="--config=file:/otel-lgtm/otelcol-carga-uy.yaml"

# 3000: Grafana; 4317 y 4318: OTLP (gRPC y HTTP)
EXPOSE 3000 4317 4318
