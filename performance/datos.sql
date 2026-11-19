-- Juego de datos de la prueba de performance: la flota del volumen objetivo (AC08) registrada en el central, para que
-- cada posicion de los nodos pase por la operativa real (vehiculo conocido, viaje en curso, permiso e ITV vigentes) y
-- las consultas del frontoffice trabajen sobre meses de historia.
--
-- Va sobre una base con solo los datos de referencia: el central arrancado con CARGAUY_DATOS=base (roles, rubros,
-- tipos de carga y normas). Lo carga openshift/reiniciar-datos.sh --perfil performance; a mano:
--
--   oc exec -i $(oc get pods -l deploymentconfig=postgres -o name | head -1) -- psql -d cargauy < performance/datos.sql
--   oc exec -i ... -- psql -d cargauy -v vehiculos=6000 -v meses=3 < performance/datos.sql
--
-- Parametros (psql -v), con sus valores por defecto:
--   vehiculos        18000  SIM0001... como los generan los nodos (nodo 1 desde 1, nodo 2 desde NODO_VEHICULOS_DESDE)
--   por_empresa      200    vehiculos por empresa: 18.000 / 200 = 90 empresas
--   meses            6      meses de historia: un viaje terminado por vehiculo y por mes, con su Guia y sus eventos
--   cada_caso        50     uno de cada N viajes terminados tiene un caso de exceso de carga notificado (0 = ninguno)
--   cada_irregular   100    uno de cada N vehiculos tiene la ITV vencida: su primera posicion del dia abre un caso
--                           "sin habilitacion" (0 = ninguno)
--   funcionarios     5      funcionarios para el escenario de k6
--
-- Determinista: las mismas entradas dan los mismos datos, salvo las fechas, que son relativas al dia de la carga.
-- Los ids arrancan en 10.000.000 para no cruzarse con los que asigna Hibernate (secuencias de a 50 desde 1). Las
-- cedulas son inventadas (7xxxxxxx). Todo va en una transaccion: si algo falla, no queda nada a medias.

\set ON_ERROR_STOP on
\if :{?vehiculos}
\else
  \set vehiculos 18000
\endif
\if :{?por_empresa}
\else
  \set por_empresa 200
\endif
\if :{?meses}
\else
  \set meses 6
\endif
\if :{?cada_caso}
\else
  \set cada_caso 50
\endif
\if :{?cada_irregular}
\else
  \set cada_irregular 100
\endif
\if :{?funcionarios}
\else
  \set funcionarios 5
\endif

BEGIN;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM empresa WHERE id >= 10000000) THEN
    RAISE EXCEPTION 'el juego de datos de performance ya esta cargado: reiniciar la base antes de volver a cargarlo';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM rubro) OR NOT EXISTS (SELECT 1 FROM rol WHERE nombre = 'CIUDADANO') THEN
    RAISE EXCEPTION 'faltan los datos de referencia: arrancar el central (CARGAUY_DATOS=base) antes de cargar esto';
  END IF;
END $$;

-- referencias que la carga usa varias veces
CREATE TEMP TABLE param ON COMMIT DROP AS
SELECT :vehiculos::int AS vehiculos,
       :por_empresa::int AS por_empresa,
       ceil(:vehiculos::numeric / :por_empresa)::int AS empresas,
       :meses::int AS meses,
       :cada_caso::int AS cada_caso,
       :cada_irregular::int AS cada_irregular,
       :funcionarios::int AS funcionarios,
       (now() AT TIME ZONE 'America/Montevideo')::date AS hoy,
       date_trunc('second', now() AT TIME ZONE 'America/Montevideo') AS ahora,
       (SELECT id FROM rol WHERE nombre = 'CIUDADANO') AS rol_ciudadano,
       (SELECT id FROM rol WHERE nombre = 'FUNCIONARIO') AS rol_funcionario,
       (SELECT array_agg(id ORDER BY id) FROM rubro) AS rubros,
       (SELECT array_agg(id ORDER BY id) FROM tipocarga) AS tipos,
       -- las CIUDADES del nodo de tracking (Flota.java), en el mismo orden
       ARRAY[-34.9011, -31.3833, -32.3214, -30.9053, -31.7110, -32.3703, -34.4626, -33.1325, -34.9000, -33.3806] AS lat,
       ARRAY[-56.1645, -57.9667, -58.0756, -55.5508, -55.9811, -54.1675, -57.8400, -58.2956, -54.9500, -56.5236] AS lon;

-- matricula como la arma el nodo: String.format("SIM%04d", n). La ruta tambien, como Flota.ruta(n): origen o y
-- destino d (indices de base 0 en las ciudades). Las Guias de los viajes en curso y asignados usan esos extremos, asi
-- las posiciones quedan sobre el tramo de la Guia y solo los vehiculos que el nodo desvia a proposito abren casos
CREATE TEMP TABLE flota ON COMMIT DROP AS
SELECT v,
       'SIM' || CASE WHEN v < 10000 THEN lpad(v::text, 4, '0') ELSE v::text END AS matricula,
       (v - 1) / p.por_empresa + 1 AS e,
       v % 10 AS o,
       (v + 1 + (v / 10) % 9) % 10 AS d
FROM param p, generate_series(1, p.vehiculos) AS v;

-- empresas, con un responsable cada una
INSERT INTO empresa (id, nroempresa, nombrepublico, razonsocial, direccionprincipal, fechaalta)
SELECT 10000000 + e, 8000 + e, 'Fletes de prueba ' || lpad(e::text, 3, '0'),
       'Fletes de prueba ' || lpad(e::text, 3, '0') || ' S.A.', 'Ruta 1 km ' || e || ', Canelones', DATE '2020-01-01'
FROM param p, generate_series(1, p.empresas) AS e;

INSERT INTO usuario (id) SELECT 10000000 + e FROM param p, generate_series(1, p.empresas) AS e;
INSERT INTO ciudadano (id, cedula, nombres, apellidos)
SELECT 10000000 + e, (70000000 + e)::text, 'Responsable', 'Prueba ' || e FROM param p, generate_series(1, p.empresas) AS e;
INSERT INTO usuario_rol (usuario_id, roles_id)
SELECT 10000000 + e, p.rol_ciudadano FROM param p, generate_series(1, p.empresas) AS e;
INSERT INTO responsable (id, fechadesde, ciudadano_id, empresa_id)
SELECT 10000000 + e, DATE '2020-01-01', 10000000 + e, 10000000 + e FROM param p, generate_series(1, p.empresas) AS e;

-- un chofer por vehiculo, de la empresa del vehiculo
INSERT INTO usuario (id) SELECT 11000000 + v FROM flota;
INSERT INTO ciudadano (id, cedula, nombres, apellidos)
SELECT 11000000 + v, (71000000 + v)::text, 'Chofer', 'Prueba ' || v FROM flota;
INSERT INTO usuario_rol (usuario_id, roles_id) SELECT 11000000 + v, p.rol_ciudadano FROM flota, param p;
INSERT INTO chofer (id, fechadesde, ciudadano_id, empresa_id)
SELECT 10000000 + v, DATE '2020-01-01', 11000000 + v, 10000000 + e FROM flota;

-- funcionarios para el escenario de k6
INSERT INTO usuario (id) SELECT 12000000 + f FROM param p, generate_series(1, p.funcionarios) AS f;
INSERT INTO funcionario (id, cedula) SELECT 12000000 + f, (72000000 + f)::text FROM param p, generate_series(1, p.funcionarios) AS f;
INSERT INTO usuario_rol (usuario_id, roles_id)
SELECT 12000000 + f, p.rol_funcionario FROM param p, generate_series(1, p.funcionarios) AS f;

-- vehiculos con permiso aprobado e ITV; uno de cada cada_irregular con la ITV vencida
INSERT INTO vehiculo (id, matricula, marca, modelo, pesovehiculo, capacidadcarga, empresa_id)
SELECT 10000000 + v, matricula, (ARRAY['Volvo', 'Scania', 'Mercedes-Benz', 'Iveco', 'DAF'])[v % 5 + 1], 'Prueba',
       8000 + (v % 10) * 100, 22000 + (v % 8) * 500, 10000000 + e
FROM flota;

INSERT INTO permiso (id, estado, nropermiso, fecharevision, validodesde, validohasta, vehiculo_id)
SELECT 10000000 + v, 'APROBADO', 'PRF-' || lpad(v::text, 6, '0'), p.ahora - interval '200 days',
       p.hoy - 200, p.hoy + 165, 10000000 + v
FROM flota, param p;

INSERT INTO itv (id, fechainspeccion, fechavencimiento, vehiculo_id)
SELECT 10000000 + v,
       CASE WHEN p.cada_irregular > 0 AND v % p.cada_irregular = 0 THEN p.hoy - 400 ELSE p.hoy - 200 END,
       CASE WHEN p.cada_irregular > 0 AND v % p.cada_irregular = 0 THEN p.hoy - 35 ELSE p.hoy + 165 END,
       10000000 + v
FROM flota, param p;

-- historia: un viaje terminado por vehiculo y por mes, con su Guia y sus eventos de inicio y fin
CREATE TEMP TABLE historia ON COMMIT DROP AS
SELECT ((m - 1) * p.vehiculos + v) AS n, f.v, f.e, m,
       (date_trunc('month', p.hoy) - make_interval(months => m))::date + (f.v % 27) AS fecha
FROM flota f, param p, generate_series(1, p.meses) AS m;

INSERT INTO guia (id, fecha, origenlat, origenlon, destinolat, destinolon, volumen, empresa_id, responsable_id, rubro_id, tipocarga_id)
SELECT 20000000 + n, fecha, -34.9011, -56.1645, -30.9053, -55.5508, 15000 + (v % 10) * 500,
       10000000 + e, 10000000 + e,
       p.rubros[(v + m) % array_length(p.rubros, 1) + 1], p.tipos[(v + 2 * m) % array_length(p.tipos, 1) + 1]
FROM historia, param p;

INSERT INTO viaje (id, estado, fechainicio, fechafin, version, chofer_id, guia_id, vehiculo_id)
SELECT 20000000 + n, 'FINALIZADO', fecha + time '08:00', fecha + time '14:00', 0, 10000000 + v, 20000000 + n, 10000000 + v
FROM historia;

INSERT INTO eventoviaje (id, idevento, idviaje, idchofer, tipo, resultado, timestampgeneracion, timestamprecepcion)
SELECT 20000000 + 2 * n - 1 + k, 'prf-' || n || (ARRAY['-inicio', '-fin'])[k + 1], 20000000 + n, 11000000 + v,
       (ARRAY['INICIO', 'FIN'])[k + 1], 'APLICADO',
       ((fecha + time '08:00' + k * interval '6 hours') AT TIME ZONE 'America/Montevideo'),
       ((fecha + time '08:00' + k * interval '6 hours' + interval '2 seconds') AT TIME ZONE 'America/Montevideo')
FROM historia, generate_series(0, 1) AS k;

-- casos de exceso de carga notificados sobre algunos viajes terminados, con su evidencia del pesaje
INSERT INTO casofiscalizacion (id, clavededuplicacion, estado, tipo, fechahecho, fechadeteccion, fechanotificacion,
                               vencedescargo, version, empresa_id, vehiculo_id, viaje_id)
SELECT 20000000 + n, 'EXCESO_CARGA|' || (10000000 + v) || '|' || fecha, 'NOTIFICADA', 'EXCESO_CARGA',
       fecha + time '11:00', fecha + time '14:01', fecha + time '14:01', fecha + time '14:01' + interval '10 days', 0,
       10000000 + e, 10000000 + v, 20000000 + n
FROM historia, param p
WHERE p.cada_caso > 0 AND n % p.cada_caso = 0;

INSERT INTO evidencia (id, caso_id, tipo, idorigen, fechaevento, valorobservado, umbral, descripcion)
SELECT 20000000 + n, 20000000 + n, 'PESAJE', 'balanzas-sur/prf-' || n, fecha + time '11:00',
       '42000', '38850', 'El pesaje supera el maximo del vehiculo con la tolerancia.'
FROM historia, param p
WHERE p.cada_caso > 0 AND n % p.cada_caso = 0;

-- el viaje en curso de cada vehiculo, que reciben sus posiciones: empezo entre 1 y 2 horas antes de la carga
INSERT INTO guia (id, fecha, origenlat, origenlon, destinolat, destinolon, volumen, empresa_id, responsable_id, rubro_id, tipocarga_id)
SELECT 10000000 + v, p.hoy, p.lat[o + 1], p.lon[o + 1], p.lat[d + 1], p.lon[d + 1], 15000 + (v % 10) * 500,
       10000000 + e, 10000000 + e,
       p.rubros[v % array_length(p.rubros, 1) + 1], p.tipos[v % array_length(p.tipos, 1) + 1]
FROM flota, param p;

INSERT INTO viaje (id, estado, fechainicio, version, chofer_id, guia_id, vehiculo_id)
SELECT 10000000 + v, 'EN_CURSO', p.ahora - interval '1 hour' - make_interval(mins => v % 60), 0,
       10000000 + v, 10000000 + v, 10000000 + v
FROM flota, param p;

INSERT INTO eventoviaje (id, idevento, idviaje, idchofer, tipo, resultado, timestampgeneracion, timestamprecepcion)
SELECT 10000000 + v, 'prf-curso-' || v, 10000000 + v, 11000000 + v, 'INICIO', 'APLICADO',
       (p.ahora - interval '1 hour' - make_interval(mins => v % 60)) AT TIME ZONE 'America/Montevideo',
       (p.ahora - interval '1 hour' - make_interval(mins => v % 60) + interval '2 seconds') AT TIME ZONE 'America/Montevideo'
FROM flota, param p;

-- el siguiente viaje de cada vehiculo, asignado al mismo chofer y con la ruta de vuelta, como el nodo al llegar al
-- destino: el escenario movil de k6 termina el viaje en curso e inicia este, como en la operativa real, donde el
-- vehiculo no queda circulando sin viaje
INSERT INTO guia (id, fecha, origenlat, origenlon, destinolat, destinolon, volumen, empresa_id, responsable_id, rubro_id, tipocarga_id)
SELECT 30000000 + v, p.hoy, p.lat[d + 1], p.lon[d + 1], p.lat[o + 1], p.lon[o + 1], 15000 + (v % 10) * 500,
       10000000 + e, 10000000 + e,
       p.rubros[v % array_length(p.rubros, 1) + 1], p.tipos[v % array_length(p.tipos, 1) + 1]
FROM flota, param p;

INSERT INTO viaje (id, estado, version, chofer_id, guia_id, vehiculo_id)
SELECT 30000000 + v, 'ASIGNADO', 0, 10000000 + v, 30000000 + v, 10000000 + v
FROM flota;

SELECT (SELECT count(*) FROM empresa WHERE id >= 10000000) AS empresas,
       (SELECT count(*) FROM vehiculo WHERE id >= 10000000) AS vehiculos,
       (SELECT count(*) FROM viaje WHERE id >= 10000000 AND estado = 'EN_CURSO') AS viajes_en_curso,
       (SELECT count(*) FROM viaje WHERE id >= 30000000 AND estado = 'ASIGNADO') AS viajes_asignados,
       (SELECT count(*) FROM viaje WHERE id BETWEEN 20000000 AND 29999999) AS viajes_terminados,
       (SELECT count(*) FROM casofiscalizacion WHERE id >= 20000000) AS casos,
       (SELECT count(*) FROM itv WHERE id >= 10000000 AND fechavencimiento < (SELECT hoy FROM param)) AS itv_vencidas;

COMMIT;

-- estadisticas al dia para el planificador: sin esto, las primeras consultas se planifican como si las tablas
-- siguieran casi vacias
ANALYZE;
